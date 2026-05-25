package com.hwlee.erp.sd.quotation.dto;

import com.hwlee.erp.sd.quotation.QuotationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record QuotationResponse(
        Long id,
        String number,
        Long customerId,
        String customerCode,
        String customerName,
        QuotationStatus status,
        LocalDate issuedDate,
        LocalDate validUntil,
        BigDecimal totalAmount,
        List<QuotationLineResponse> lines,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
