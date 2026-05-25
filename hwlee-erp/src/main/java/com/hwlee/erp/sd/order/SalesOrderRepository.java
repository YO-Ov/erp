package com.hwlee.erp.sd.order;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesOrderRepository
        extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder> {

    Optional<SalesOrder> findByNumber(String number);

    /**
     * 한 고객의 "한도에 영향을 주는" 활성 수주의 합계.
     * 신용한도 검증 = (creditLimit) - (이 합계) >= 신규 수주 totalAmount.
     */
    @Query("""
        select coalesce(sum(o.totalAmount), 0)
          from SalesOrder o
         where o.customer.id = :customerId
           and o.status in (
                com.hwlee.erp.sd.order.SalesOrderStatus.CONFIRMED,
                com.hwlee.erp.sd.order.SalesOrderStatus.SHIPPING,
                com.hwlee.erp.sd.order.SalesOrderStatus.SHIPPED,
                com.hwlee.erp.sd.order.SalesOrderStatus.INVOICING,
                com.hwlee.erp.sd.order.SalesOrderStatus.INVOICED
           )
           and (:excludeId is null or o.id <> :excludeId)
        """)
    BigDecimal sumActiveOrderAmountByCustomer(@Param("customerId") Long customerId,
                                              @Param("excludeId") Long excludeId);
}
