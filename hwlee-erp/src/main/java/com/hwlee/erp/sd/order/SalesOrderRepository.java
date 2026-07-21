package com.hwlee.erp.sd.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesOrderRepository
        extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder> {

    Optional<SalesOrder> findByNumber(String number);

    // ── 영업 대시보드 집계 ──

    /** 상태별 건수·금액 (파이프라인). */
    @Query("select o.status as status, count(o) as count, coalesce(sum(o.totalAmount), 0) as amount "
            + "from SalesOrder o group by o.status")
    List<SalesOrderStatusCount> aggregateByStatus();

    /** 상태별 건수·금액 (파이프라인) — 수주일 기간 필터. */
    @Query("select o.status as status, count(o) as count, coalesce(sum(o.totalAmount), 0) as amount "
            + "from SalesOrder o where o.orderDate between :from and :to group by o.status")
    List<SalesOrderStatusCount> aggregateByStatusBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** 기간 내 수주 건수 (이번 달 수주). */
    long countByOrderDateBetween(LocalDate from, LocalDate to);

    /** 기간 내 수주 금액 합계. */
    @Query("select coalesce(sum(o.totalAmount), 0) from SalesOrder o where o.orderDate between :from and :to")
    BigDecimal sumAmountByOrderDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** 최근 수주 5건. */
    List<SalesOrder> findTop5ByOrderByOrderDateDescIdDesc();

    /**
     * 한 고객의 "아직 청구(인보이스 발행) 전인" 활성 수주 합계 = 여신사용액의 ① 미청구 백로그 항.
     *
     * <p>CONFIRMED·SHIPPING·SHIPPED 만 포함한다. 청구 단계(INVOICING·INVOICED)로 넘어간 금액은
     * 매출채권(AR)으로 이관되어 {@code 미수금(발행 인보이스 − 입금)} 쪽에서 잡히므로 여기서 제외한다.
     * 이렇게 분리해야 입금이 들어오면 여신사용액이 자동으로 줄어든다
     * ({@link com.hwlee.erp.sd.order.creditcheck.CreditExposureCalculator} 참고).
     */
    @Query("""
        select coalesce(sum(o.totalAmount), 0)
          from SalesOrder o
         where o.customer.id = :customerId
           and o.status in (
                com.hwlee.erp.sd.order.SalesOrderStatus.CONFIRMED,
                com.hwlee.erp.sd.order.SalesOrderStatus.SHIPPING,
                com.hwlee.erp.sd.order.SalesOrderStatus.SHIPPED
           )
           and (:excludeId is null or o.id <> :excludeId)
        """)
    BigDecimal sumUninvoicedActiveOrderAmount(@Param("customerId") Long customerId,
                                              @Param("excludeId") Long excludeId);
}
