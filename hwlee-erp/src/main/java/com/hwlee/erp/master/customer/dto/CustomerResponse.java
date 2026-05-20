package com.hwlee.erp.master.customer.dto;

import com.hwlee.erp.common.entity.MasterStatus;
import com.hwlee.erp.master.customer.PaymentTerms;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        String code,
        String name,
        String businessNo,
        String address,
        BigDecimal creditLimit,
        PaymentTerms paymentTerms,
        MasterStatus status,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
