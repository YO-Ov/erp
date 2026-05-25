package com.hwlee.erp.sd.quotation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface QuotationRepository
        extends JpaRepository<Quotation, Long>, JpaSpecificationExecutor<Quotation> {
}
