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
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
        return mapper.toResponse(gi);
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
