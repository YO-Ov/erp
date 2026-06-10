package com.hwlee.erp.sd.order.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * 수주 확정 사건 — {@link com.hwlee.erp.sd.order.SalesOrderService#confirm} 가 발행한다.
 *
 * <p>PP(생산) 모듈이 구독해, 완제품별 <b>주문량 vs 현재고</b>를 비교하고 부족분만큼 계획오더(MRP 제안)를
 * 자동 생성한다. 이벤트는 발행자(SD)가 소유하며, PP 가 이 타입을 import 해 구독할 뿐이다 — 의존 방향은
 * {@code PP → SD} 단방향. {@link com.hwlee.erp.sd.delivery.event.DeliveryShippedEvent} 와 같은 패턴.
 *
 * <p>본문에 (itemId, orderQty) 라인을 함께 담아, 리스너가 같은 트랜잭션에서 즉시 쓰도록 한다.
 */
public record SalesOrderConfirmedEvent(
        Long salesOrderId,
        String salesOrderNumber,
        List<Line> lines
) {
    public record Line(Long itemId, BigDecimal orderQty) {}
}
