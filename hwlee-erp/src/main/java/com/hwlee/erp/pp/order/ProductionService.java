package com.hwlee.erp.pp.order;

import com.hwlee.erp.common.code.TransactionNumberGenerator;
import com.hwlee.erp.fi.journal.AutoJournalService;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemRepository;
import com.hwlee.erp.master.item.ItemType;
import com.hwlee.erp.mm.stock.MovementReason;
import com.hwlee.erp.mm.stock.Stock;
import com.hwlee.erp.mm.stock.StockMovement;
import com.hwlee.erp.mm.stock.StockMovementRepository;
import com.hwlee.erp.mm.stock.StockRepository;
import com.hwlee.erp.mm.warehouse.Warehouse;
import com.hwlee.erp.mm.warehouse.WarehouseRepository;
import com.hwlee.erp.pp.bom.Bom;
import com.hwlee.erp.pp.bom.BomRepository;
import com.hwlee.erp.pp.order.dto.MaterialAvailabilityResponse;
import com.hwlee.erp.pp.order.event.ProductionOrderCancelledEvent;
import com.hwlee.erp.pp.order.dto.ProductionOrderCreateRequest;
import com.hwlee.erp.pp.order.dto.ProductionOrderLineResponse;
import com.hwlee.erp.pp.order.dto.ProductionOrderResponse;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 생산 서비스 (Phase 8 PP) — 생산지시 생성(BOM 전개) · 착수 · 완료(재고 변신) · 취소.
 *
 * <p>완료 시점에 부품을 출고(stock.issue)하고 완제품을 입고(stock.receive)한다. 재고 메커니즘은
 * MM(Stock·StockMovement·비관 락)을 재사용하되 GoodsReceipt/GoodsIssue 헤더는 쓰지 않는다
 * (자가생산 완제품엔 vendor 가 없고, 매입/매출원가 전표 오트리거를 피하려는 것). 완제품 원가 =
 * 투입 부품 실제(이동평균) 원가 합 ÷ 수량 — 원가 보존. 회계는 {@link AutoJournalService} 직접 호출
 * (차)제품 / 대)원재료).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductionService {

    private static final String REF_TYPE = "PROD"; // stock_movement.ref_type 는 VARCHAR(10)

    private final ProductionOrderRepository orderRepository;
    private final BomRepository bomRepository;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AutoJournalService autoJournal;
    private final TransactionNumberGenerator numberGenerator;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /** 생산지시 생성 — 완제품의 BOM 을 전개해 소요 자재 라인을 굳힌다(DRAFT=PLANNED). */
    @Transactional
    public ProductionOrderResponse createDraft(ProductionOrderCreateRequest req) {
        Item product = item(req.productItemId());
        if (product.getItemType() != ItemType.FINISHED)
            throw new IllegalArgumentException("완제품(FINISHED) 만 생산할 수 있습니다: " + product.getCode());
        Warehouse warehouse = warehouseRepository.findById(req.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found: id=" + req.warehouseId()));

        List<Bom> boms = bomRepository.findByProductId(product.getId());
        if (boms.isEmpty())
            throw new IllegalStateException("BOM 이 등록되지 않은 완제품은 생산지시를 낼 수 없습니다: " + product.getCode());

        String number = numberGenerator.nextProductionOrderNumber(req.orderDate());
        ProductionOrder po = ProductionOrder.draft(
                number, product, warehouse, req.quantity(), req.orderDate(), req.dueDate());
        for (Bom bom : boms) {
            BigDecimal requiredQty = bom.getQuantity().multiply(req.quantity());
            po.addLine(bom.getComponent(), requiredQty);
        }
        ProductionOrder saved = orderRepository.save(po);
        return toResponse(reload(saved.getId()));
    }

    public ProductionOrderResponse findById(Long id) {
        return toResponse(reload(id));
    }

    public Page<ProductionOrderResponse> search(ProductionOrderStatus status, Long productItemId,
                                                LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        Specification<ProductionOrder> spec = (root, query, cb) -> cb.conjunction();
        if (status != null)
            spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        if (productItemId != null)
            spec = spec.and((r, q, cb) -> cb.equal(r.get("product").get("id"), productItemId));
        if (dateFrom != null)
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("orderDate"), dateFrom));
        if (dateTo != null)
            spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("orderDate"), dateTo));
        return orderRepository.findAll(spec, pageable).map(this::toResponse);
    }

    /** 부품 가용성(소요량 vs 현재고) — 막지 않는 참고 조회. */
    public MaterialAvailabilityResponse materialAvailability(Long id) {
        ProductionOrder po = reload(id);
        Long whId = po.getWarehouse().getId();
        List<MaterialAvailabilityResponse.Line> lines = new ArrayList<>();
        boolean producible = true;
        for (ProductionOrderLine line : po.getLines()) {
            Item c = line.getComponent();
            BigDecimal onHand = stockRepository.findByItemIdAndWarehouseId(c.getId(), whId)
                    .map(Stock::getQtyOnHand).orElse(BigDecimal.ZERO);
            boolean sufficient = onHand.compareTo(line.getRequiredQty()) >= 0;
            if (!sufficient) producible = false;
            lines.add(new MaterialAvailabilityResponse.Line(
                    c.getId(), c.getCode(), c.getName(), line.getRequiredQty(), onHand, sufficient));
        }
        return new MaterialAvailabilityResponse(producible, lines);
    }

    /** PLANNED → RELEASED. */
    @Transactional
    public ProductionOrderResponse release(Long id) {
        ProductionOrder po = orderRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        po.release();
        return toResponse(reload(id));
    }

    /**
     * RELEASED → COMPLETED. ⭐ 부품 출고(stock.issue) + 완제품 입고(stock.receive) + 생산 분개.
     * 한 부품이라도 재고 부족이면 InsufficientStockException 으로 전체 롤백(부분 출고 없음).
     */
    @Transactional
    public ProductionOrderResponse complete(Long id) {
        ProductionOrder po = orderRepository.findByIdWithLines(id).orElseThrow(() -> notFound(id));
        Warehouse warehouse = po.getWarehouse();
        LocalDateTime now = LocalDateTime.now(clock);

        // ① 부품 출고 — 라인별 비관 락 + 가용 검증 + 이동평균 단가 회수
        BigDecimal totalCost = BigDecimal.ZERO;
        for (ProductionOrderLine line : po.getLines()) {
            Item component = line.getComponent();
            Stock stock = stockRepository.findForUpdate(component.getId(), warehouse.getId())
                    .orElseThrow(() -> new com.hwlee.erp.mm.stock.InsufficientStockException(
                            component.getId(), warehouse.getId(), BigDecimal.ZERO, line.getRequiredQty()));
            BigDecimal appliedCost = stock.issue(line.getRequiredQty()); // 부족 시 예외 → 전체 롤백
            line.recordIssuedCost(appliedCost);
            totalCost = totalCost.add(appliedCost.multiply(line.getRequiredQty()));
            stockMovementRepository.save(StockMovement.of(
                    component, warehouse, line.getRequiredQty().negate(), appliedCost,
                    MovementReason.PRODUCTION_OUT, REF_TYPE, po.getId(), now));
        }

        // ② 완제품 입고 — 단위원가 = 투입 부품 원가 합 ÷ 수량 (원가 보존)
        BigDecimal unitCost = totalCost.divide(po.getQuantity(), 2, RoundingMode.HALF_UP);
        Item product = po.getProduct();
        Stock finishedStock = stockRepository.findByItemIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> stockRepository.save(Stock.empty(product, warehouse)));
        finishedStock.receive(po.getQuantity(), unitCost);
        stockMovementRepository.save(StockMovement.of(
                product, warehouse, po.getQuantity(), unitCost,
                MovementReason.PRODUCTION_IN, REF_TYPE, po.getId(), now));

        // ③ 상태 전이 + 회계 분개 (차)제품 / 대)원재료, 직접재료비 = totalCost)
        po.complete(now);
        autoJournal.createProductionEntry(po.getId(), po.getNumber(), LocalDate.now(clock), totalCost);

        log.info("생산 완료: {} (완제품 {} x{}, 직접재료비={}, 단위원가={})",
                po.getNumber(), product.getCode(), po.getQuantity(), totalCost, unitCost);
        return toResponse(reload(id));
    }

    @Transactional
    public ProductionOrderResponse cancel(Long id) {
        ProductionOrder po = orderRepository.findById(id).orElseThrow(() -> notFound(id));
        po.cancel();
        // ⭐ 생산지시 취소 사건 발행 — planning 리스너가 계획오더(MRP)를 검토 대기로 되살린다.
        events.publishEvent(new ProductionOrderCancelledEvent(po.getId(), po.getNumber()));
        return toResponse(reload(id));
    }

    // ── 내부 ─────────────────────────────────────────────
    private ProductionOrder reload(Long id) {
        return orderRepository.findByIdWithLines(id).orElseThrow(() -> notFound(id));
    }

    private Item item(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: id=" + id));
    }

    private static EntityNotFoundException notFound(Long id) {
        return new EntityNotFoundException("ProductionOrder not found: id=" + id);
    }

    private ProductionOrderResponse toResponse(ProductionOrder po) {
        List<ProductionOrderLineResponse> lines = po.getLines().stream()
                .map(l -> new ProductionOrderLineResponse(
                        l.getId(), l.getLineNo(),
                        l.getComponent().getId(), l.getComponent().getCode(), l.getComponent().getName(),
                        l.getRequiredQty(), l.getIssuedUnitCost()))
                .toList();
        return new ProductionOrderResponse(
                po.getId(), po.getNumber(),
                po.getProduct().getId(), po.getProduct().getCode(), po.getProduct().getName(),
                po.getWarehouse().getId(), po.getWarehouse().getName(),
                po.getQuantity(), po.getStatus(), po.getOrderDate(), po.getDueDate(), po.getCompletedAt(),
                lines,
                po.getCreatedAt(), po.getCreatedBy(), po.getUpdatedAt(), po.getUpdatedBy());
    }
}
