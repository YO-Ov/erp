package com.hwlee.erp.mm.goodsissue;

import com.hwlee.erp.common.code.TransactionNumberGenerator;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemRepository;
import com.hwlee.erp.mm.goodsissue.dto.GoodsIssueCreateRequest;
import com.hwlee.erp.mm.goodsissue.dto.GoodsIssueLineRequest;
import com.hwlee.erp.mm.goodsissue.dto.GoodsIssueResponse;
import com.hwlee.erp.mm.goodsissue.dto.GoodsIssueUpdateRequest;
import com.hwlee.erp.mm.stock.MovementReason;
import com.hwlee.erp.mm.stock.Stock;
import com.hwlee.erp.mm.stock.StockMovement;
import com.hwlee.erp.mm.stock.StockMovementRepository;
import com.hwlee.erp.mm.stock.StockRepository;
import com.hwlee.erp.mm.warehouse.Warehouse;
import com.hwlee.erp.mm.warehouse.WarehouseRepository;
import com.hwlee.erp.sd.delivery.event.DeliveryShippedEvent;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 출고 서비스 — 헤더 관리 + {@link #post} 시 비관적 락으로 Stock 차감 + StockMovement 적재.
 *
 * <p>{@link #post} 의 핵심:
 * <ol>
 *   <li>{@link StockRepository#findForUpdate} 로 (item, warehouse) 행 비관 락 점유.</li>
 *   <li>가용 검증 (Stock.issue 내부) — 부족하면 {@code InsufficientStockException} → 409.</li>
 *   <li>{@code stock.issue(qty)} 차감 + 적용 단가 반환 (= 직전 평균).</li>
 *   <li>{@link StockMovement} (-) 행 적재.</li>
 * </ol>
 * 동시 출고는 비관 락으로 직렬화 — 음수 재고 발생 자체가 불가능.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoodsIssueService {

    private final GoodsIssueRepository repository;
    private final GoodsIssueMapper mapper;
    private final WarehouseRepository warehouseRepository;
    private final ItemRepository itemRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final TransactionNumberGenerator numberGenerator;
    private final Clock clock;

    @Transactional
    public GoodsIssueResponse create(GoodsIssueCreateRequest req) {
        Warehouse warehouse = warehouseRepository.findById(req.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found: id=" + req.warehouseId()));
        String number = numberGenerator.nextGoodsIssueNumber(req.issueDate());
        GoodsIssue gi = GoodsIssue.draft(number, warehouse, req.issueDate(), req.reason());
        addLines(gi, req.lines());
        return mapper.toResponse(repository.save(gi));
    }

    public GoodsIssueResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    public Page<GoodsIssueResponse> search(Specification<GoodsIssue> spec, Pageable pageable) {
        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional
    public GoodsIssueResponse update(Long id, GoodsIssueUpdateRequest req) {
        GoodsIssue gi = getOrThrow(id);
        Warehouse warehouse = warehouseRepository.findById(req.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found: id=" + req.warehouseId()));
        gi.updateHeader(warehouse, req.issueDate(), req.reason());
        gi.clearLines();
        addLines(gi, req.lines());
        return mapper.toResponse(gi);
    }

    @Transactional
    public GoodsIssueResponse post(Long id) {
        GoodsIssue gi = getOrThrow(id);
        postStock(gi);
        return mapper.toResponse(gi);
    }

    /**
     * DRAFT → POSTED 전이 + 라인별 비관 락 재고 차감 + StockMovement(-) 적재.
     * 직접 출고({@link #post})와 출하 연계({@link #createAndPostFromDelivery}) 가 공유한다.
     */
    private void postStock(GoodsIssue gi) {
        gi.post(LocalDateTime.now(clock));
        Warehouse warehouse = gi.getWarehouse();
        LocalDateTime now = LocalDateTime.now(clock);

        for (GoodsIssueLine line : gi.getLines()) {
            Item item = line.getItem();
            Stock stock = stockRepository
                    .findForUpdate(item.getId(), warehouse.getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Stock not found: item=" + item.getId() + ", warehouse=" + warehouse.getId()
                                    + " — 출고 전에 입고가 있어야 합니다."));
            BigDecimal appliedCost = stock.issue(line.getQuantity());

            stockMovementRepository.save(StockMovement.of(
                    item, warehouse, line.getQuantity().negate(), appliedCost,
                    MovementReason.GOODS_ISSUE, "GI", gi.getId(), now));
        }
    }

    /**
     * 출하 확정 사건({@link DeliveryShippedEvent}) 으로부터 GoodsIssue 를 생성하고 즉시 확정한다 (Phase 4).
     *
     * <p>호출자는 {@code DeliveryEventListener.onShipped} — {@code @TransactionalEventListener(BEFORE_COMMIT)}
     * 이므로 이 메서드는 출하 트랜잭션에 그대로 참여({@code REQUIRED})한다. 가용 부족이면
     * {@code InsufficientStockException} 이 호출자(=출하 트랜잭션)로 전파되어 전체가 롤백된다.
     */
    @Transactional
    public GoodsIssueResponse createAndPostFromDelivery(DeliveryShippedEvent event) {
        Warehouse warehouse = warehouseRepository.findById(event.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Warehouse not found: id=" + event.warehouseId()));

        String number = numberGenerator.nextGoodsIssueNumber(event.shippedDate());
        GoodsIssue gi = GoodsIssue.draftForDelivery(
                number, warehouse, event.shippedDate(), event.deliveryId());

        for (DeliveryShippedEvent.Line line : event.lines()) {
            Item item = itemRepository.findById(line.itemId())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found: id=" + line.itemId()));
            gi.addLine(item, line.quantity());
        }

        repository.save(gi);   // id 채번 — StockMovement.refId 에 박기 위해 post 전에 저장
        postStock(gi);
        return mapper.toResponse(gi);
    }

    /**
     * 출하 취소 사건({@code DeliveryCancelledEvent}) 으로부터 연결된 GoodsIssue 를 취소한다 (Phase 4).
     *
     * <p>방어적 — 연결된 GI 가 없거나(Phase 4 적용 전 출하) 이미 POSTED 가 아니면(이미 취소됨) 조용히 무시한다.
     */
    @Transactional
    public void cancelByDeliveryId(Long deliveryId) {
        GoodsIssue gi = repository.findByDeliveryId(deliveryId).orElse(null);
        if (gi == null) {
            log.info("출하 id={} 에 연결된 GoodsIssue 없음 — 취소 건너뜀", deliveryId);
            return;
        }
        if (gi.getStatus() != GoodsIssueStatus.POSTED) {
            log.info("GoodsIssue id={} 가 POSTED 아님(현재 {}) — 취소 건너뜀", gi.getId(), gi.getStatus());
            return;
        }
        cancel(gi.getId());   // 기존 취소 로직 재사용 — 비관 락 + 재고 복원 + ADJUSTMENT_PLUS
    }

    /**
     * POSTED → CANCELLED. 차감했던 수량을 다시 더한다 (평균은 건드리지 않음).
     */
    @Transactional
    public GoodsIssueResponse cancel(Long id) {
        GoodsIssue gi = getOrThrow(id);
        gi.cancel();
        Warehouse warehouse = gi.getWarehouse();
        LocalDateTime now = LocalDateTime.now(clock);

        for (GoodsIssueLine line : gi.getLines()) {
            Item item = line.getItem();
            // 취소도 비관 락 — 그 사이 다른 출고와의 race 방지 (avg 조회 안전성).
            Stock stock = stockRepository
                    .findForUpdate(item.getId(), warehouse.getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Stock not found: item=" + item.getId() + ", warehouse=" + warehouse.getId()));
            BigDecimal appliedCost = stock.getAverageCost();
            stock.cancelIssue(line.getQuantity());

            stockMovementRepository.save(StockMovement.of(
                    item, warehouse, line.getQuantity(), appliedCost,
                    MovementReason.ADJUSTMENT_PLUS, "GI", gi.getId(), now));
        }
        return mapper.toResponse(gi);
    }

    private void addLines(GoodsIssue gi, List<GoodsIssueLineRequest> lineReqs) {
        for (GoodsIssueLineRequest lineReq : lineReqs) {
            Item item = itemRepository.findById(lineReq.itemId())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found: id=" + lineReq.itemId()));
            gi.addLine(item, lineReq.quantity());
        }
    }

    private GoodsIssue getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("GoodsIssue not found: id=" + id));
    }
}
