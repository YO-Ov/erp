package com.hwlee.erp.sd.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository
        extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    /** Phase 9 — 일일 매출 마감: 기준일에 발행(ISSUED)된 인보이스. */
    @Query("select i from Invoice i "
            + "where i.invoiceDate = :date "
            + "and i.status = com.hwlee.erp.sd.invoice.InvoiceStatus.ISSUED")
    List<Invoice> findIssuedByInvoiceDate(@Param("date") LocalDate date);

    /** Phase 9 — 채권 노령화: 기준일까지 발행된 모든 ISSUED 인보이스(고객까지 fetch). */
    @Query("select i from Invoice i "
            + "join fetch i.salesOrder so "
            + "join fetch so.customer "
            + "where i.status = com.hwlee.erp.sd.invoice.InvoiceStatus.ISSUED "
            + "and i.invoiceDate <= :date "
            + "order by i.invoiceDate asc, i.id asc")
    List<Invoice> findIssuedUpToWithCustomer(@Param("date") LocalDate date);

    /** 여신 미수금 산정: 한 고객에게 발행(ISSUED)된 인보이스 합계(부가세 포함). 입금 합계와 차감해 미수금 도출. */
    @Query("select coalesce(sum(i.totalAmount), 0) from Invoice i "
            + "where i.salesOrder.customer.id = :customerId "
            + "and i.status = com.hwlee.erp.sd.invoice.InvoiceStatus.ISSUED")
    BigDecimal sumIssuedInvoiceTotalByCustomer(@Param("customerId") Long customerId);

    /** Phase 10 — 매출 리포트: 기간 내 발행(ISSUED) 인보이스. 일/월 집계는 서비스에서 그룹핑. */
    @Query("select i from Invoice i "
            + "where i.status = com.hwlee.erp.sd.invoice.InvoiceStatus.ISSUED "
            + "and i.invoiceDate between :from and :to "
            + "order by i.invoiceDate asc, i.id asc")
    List<Invoice> findIssuedBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
