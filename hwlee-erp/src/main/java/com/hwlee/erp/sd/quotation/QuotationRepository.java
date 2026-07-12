package com.hwlee.erp.sd.quotation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface QuotationRepository
        extends JpaRepository<Quotation, Long>, JpaSpecificationExecutor<Quotation> {

    /** 상태별 건수 — 대시보드 '발송 대기(APPROVED)' 카운트용. */
    long countByStatus(QuotationStatus status);
}
