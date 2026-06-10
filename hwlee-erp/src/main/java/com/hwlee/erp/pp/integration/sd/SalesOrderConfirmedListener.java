package com.hwlee.erp.pp.integration.sd;

import com.hwlee.erp.pp.planning.PlannedOrderService;
import com.hwlee.erp.sd.order.event.SalesOrderConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * SD → PP 통합 — 수주 확정 사건을 받아 계획오더(MRP 제안)를 자동 생성한다.
 *
 * <p>패키지 위치 {@code pp/integration/sd} = "SD 로부터 들어오는 통합"(MM 의
 * {@code mm/integration/sd} 와 같은 규약). {@code BEFORE_COMMIT} 이라 수주 확정 트랜잭션 안에서
 * 돌고, 여기서 예외가 나면 수주 확정도 함께 롤백된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class SalesOrderConfirmedListener {

    private final PlannedOrderService plannedOrderService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void onConfirmed(SalesOrderConfirmedEvent event) {
        log.info("수주 확정 사건 수신: salesOrderId={}, number={}",
                event.salesOrderId(), event.salesOrderNumber());
        plannedOrderService.createFromSalesOrder(event);
    }
}
