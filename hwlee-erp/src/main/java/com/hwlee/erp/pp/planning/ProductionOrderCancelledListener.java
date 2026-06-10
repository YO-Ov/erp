package com.hwlee.erp.pp.planning;

import com.hwlee.erp.pp.order.event.ProductionOrderCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * PP 내부 연계 — 생산지시 취소 사건을 받아 원래 계획오더(MRP)를 검토 대기로 되살린다.
 *
 * <p>order → planning 직접 호출 대신 이벤트를 쓰는 이유: planning 은 이미 order
 * (ProductionService.createDraft)를 호출하므로, 반대 방향까지 직접 의존하면 순환이 된다.
 * {@code BEFORE_COMMIT} 이라 생산지시 취소 트랜잭션 안에서 함께 반영된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ProductionOrderCancelledListener {

    private final PlannedOrderService plannedOrderService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void onCancelled(ProductionOrderCancelledEvent event) {
        plannedOrderService.revertByProductionNumber(event.productionNumber());
    }
}
