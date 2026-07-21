package com.hwlee.erp.mm.purchaseorder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    // ── 구매 대시보드 집계 ──
    // 발주 금액은 저장 컬럼이 아니라 라인 합(PurchaseOrderLine.lineTotal)이므로,
    // 금액 집계는 라인을 left join 해 sum 한다(라인 없는 DRAFT 도 건수엔 잡히도록 left join + count(distinct)).

    /** 기간 내 발주 건수 (이번 달 발주 — 발주일 기준). */
    long countByOrderDateBetween(LocalDate from, LocalDate to);

    /** 기간 내 발주 금액 합계(라인 합). */
    @Query("select coalesce(sum(l.lineTotal), 0) "
            + "from PurchaseOrderLine l where l.purchaseOrder.orderDate between :from and :to")
    BigDecimal sumAmountByOrderDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** 상태별 건수·금액 (파이프라인). */
    @Query("select o.status as status, count(distinct o) as count, coalesce(sum(l.lineTotal), 0) as amount "
            + "from PurchaseOrder o left join o.lines l group by o.status")
    List<PurchaseOrderStatusCount> aggregateByStatus();

    /** 최근 발주 5건. */
    List<PurchaseOrder> findTop5ByOrderByOrderDateDescIdDesc();
}
