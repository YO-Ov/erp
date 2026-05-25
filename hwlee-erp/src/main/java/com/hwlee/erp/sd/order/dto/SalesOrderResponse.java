package com.hwlee.erp.sd.order.dto;

import com.hwlee.erp.sd.order.SalesOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SalesOrderResponse(
        Long id,
        String number,
        Long customerId,
        String customerCode,
        String customerName,
        Long salespersonId,
        String salespersonName,
        Long quotationId,
        String quotationNumber,
        SalesOrderStatus status,
        LocalDate orderDate,
        LocalDateTime confirmedAt,
        BigDecimal totalAmount,
        List<SalesOrderLineResponse> lines,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
