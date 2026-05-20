package com.hwlee.erp.master.vendor.dto;

import com.hwlee.erp.common.entity.MasterStatus;
import com.hwlee.erp.master.customer.PaymentTerms;
import java.time.LocalDateTime;

public record VendorResponse(
        Long id,
        String code,
        String name,
        String businessNo,
        String address,
        PaymentTerms paymentTerms,
        MasterStatus status,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
