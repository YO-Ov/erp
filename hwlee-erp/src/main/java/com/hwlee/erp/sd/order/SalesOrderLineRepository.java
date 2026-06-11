package com.hwlee.erp.sd.order;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesOrderLineRepository extends JpaRepository<SalesOrderLine, Long> {

    /**
     * 영업 ATP — 한 품목의 "이미 약속됐지만 아직 출하 안 된" 확정수주 수량 합.
     * 확정(CONFIRMED)부터 청구완료(INVOICED)까지의 라인에서 {@code orderQty - shippedQty} 를 더한다.
     * (DRAFT 는 아직 확정 전이라 약속으로 보지 않는다 — 신용한도 검증과 같은 사상.)
     */
    @Query("""
        select coalesce(sum(l.orderQty - l.shippedQty), 0)
          from SalesOrderLine l
         where l.item.id = :itemId
           and l.salesOrder.status in (
                com.hwlee.erp.sd.order.SalesOrderStatus.CONFIRMED,
                com.hwlee.erp.sd.order.SalesOrderStatus.SHIPPING,
                com.hwlee.erp.sd.order.SalesOrderStatus.SHIPPED,
                com.hwlee.erp.sd.order.SalesOrderStatus.INVOICING,
                com.hwlee.erp.sd.order.SalesOrderStatus.INVOICED
           )
        """)
    BigDecimal sumUnshippedCommittedByItem(@Param("itemId") Long itemId);
}
