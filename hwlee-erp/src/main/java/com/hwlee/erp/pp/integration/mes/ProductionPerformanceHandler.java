package com.hwlee.erp.pp.integration.mes;

import com.hwlee.erp.fi.journal.AutoJournalService;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemRepository;
import com.hwlee.erp.mm.stock.MovementReason;
import com.hwlee.erp.mm.stock.Stock;
import com.hwlee.erp.mm.stock.StockMovement;
import com.hwlee.erp.mm.stock.StockMovementRepository;
import com.hwlee.erp.mm.stock.StockRepository;
import com.hwlee.erp.mm.warehouse.Warehouse;
import com.hwlee.erp.pp.order.ProductionOrder;
import com.hwlee.erp.pp.order.ProductionOrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MES 생산 실적 이벤트를 ERP 재고/회계에 반영 — Phase 8 생산완료 로직의 재사용.
 *
 * <p>한 트랜잭션 안에서: 멱등 체크 → 자재 차감(부품 출고) → 완제품 입고 → 생산 원가 분개 → 처리기록.
 * 멱등(이미 처리한 eventId)이면 아무 것도 하지 않고 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionPerformanceHandler {

    private static final String SOURCE = "MES";
    private static final String REF_TYPE = "PROD";

    private final ProcessedEventRepository processedRepository;
    private final ProductionOrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AutoJournalService autoJournalService;

    @Transactional
    public void handle(ProductionPerformanceMessage msg) {
        if (processedRepository.existsByEventId(msg.eventId())) {
            log.info("[MES 실적수신] 멱등 — 이미 처리됨 eventId={}", msg.eventId());
            return;
        }

        ProductionOrder po = orderRepository.findByNumber(msg.erpOrderNo())
                .orElseThrow(() -> new IllegalStateException("생산지시를 찾을 수 없습니다: " + msg.erpOrderNo()));
        Warehouse warehouse = po.getWarehouse();
        LocalDateTime now = LocalDateTime.now();

        // ① 자재 차감(부품 출고) — 비관 락 + 이동평균 단가 회수
        BigDecimal totalCost = BigDecimal.ZERO;
        if (msg.consumptions() != null) {
            for (ProductionPerformanceMessage.ConsumptionLine c : msg.consumptions()) {
                Item component = itemRepository.findByCode(c.componentCode())
                        .orElseThrow(() -> new IllegalStateException("부품을 찾을 수 없습니다: " + c.componentCode()));
                Stock stock = stockRepository.findForUpdate(component.getId(), warehouse.getId())
                        .orElseThrow(() -> new IllegalStateException(
                                "부품 재고가 없습니다: " + c.componentCode()));
                BigDecimal applied = stock.issue(c.consumedQty());
                totalCost = totalCost.add(applied.multiply(c.consumedQty()));
                stockMovementRepository.save(StockMovement.of(component, warehouse,
                        c.consumedQty().negate(), applied, MovementReason.PRODUCTION_OUT,
                        REF_TYPE, po.getId(), now));
            }
        }

        // ② 완제품 입고 (양품 수량 > 0)
        if (msg.goodQty() != null && msg.goodQty().signum() > 0) {
            BigDecimal unitCost = totalCost.signum() > 0
                    ? totalCost.divide(msg.goodQty(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            Item product = po.getProduct();
            Stock finishedStock = stockRepository.findByItemIdAndWarehouseId(product.getId(), warehouse.getId())
                    .orElseGet(() -> stockRepository.save(Stock.empty(product, warehouse)));
            finishedStock.receive(msg.goodQty(), unitCost);
            stockMovementRepository.save(StockMovement.of(product, warehouse,
                    msg.goodQty(), unitCost, MovementReason.PRODUCTION_IN, REF_TYPE, po.getId(), now));

            // ③ 생산 원가 분개 (차)제품 / 대)원재료 = 직접재료비)
            if (totalCost.signum() > 0) {
                autoJournalService.createProductionEntry(po.getId(), po.getNumber(), now.toLocalDate(), totalCost);
            }
        }

        processedRepository.save(ProcessedEvent.of(msg.eventId(), SOURCE, now));
        log.info("[MES 실적수신] 반영 완료 eventId={} po={} good={} 자재원가={}",
                msg.eventId(), msg.erpOrderNo(), msg.goodQty(), totalCost);
    }
}
