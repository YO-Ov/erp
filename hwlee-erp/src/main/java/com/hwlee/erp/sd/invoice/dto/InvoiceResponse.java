package com.hwlee.erp.sd.invoice.dto;

import com.hwlee.erp.sd.invoice.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
        Long id,
        String number,
        Long salesOrderId,
        String salesOrderNumber,
        InvoiceStatus status,
        LocalDate invoiceDate,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        List<InvoiceLineResponse> lines,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
