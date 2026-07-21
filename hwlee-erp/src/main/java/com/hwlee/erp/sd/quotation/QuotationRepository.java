package com.hwlee.erp.sd.quotation;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuotationRepository
        extends JpaRepository<Quotation, Long>, JpaSpecificationExecutor<Quotation> {

    /** 상태별 건수 — 대시보드 '발송 대기(APPROVED)' 카운트용. */
    long countByStatus(QuotationStatus status);

    // ── 기간 집계 ("이번 달 견적 합계") — 기준일은 발행일(issuedDate). ──

    /** 기간 내 견적 건수. */
    long countByIssuedDateBetween(LocalDate from, LocalDate to);

    /** 기간 내 견적 금액 합계. 건이 없으면 null 이 아니라 0. */
    @Query("select coalesce(sum(q.totalAmount), 0) from Quotation q "
            + "where q.issuedDate between :from and :to")
    BigDecimal sumAmountByIssuedDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
