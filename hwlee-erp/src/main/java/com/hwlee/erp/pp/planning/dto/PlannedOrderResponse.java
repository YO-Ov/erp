package com.hwlee.erp.pp.planning.dto;

import com.hwlee.erp.pp.planning.PlannedOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PlannedOrderResponse(
        Long id,
        String number,
        Long productItemId,
        String productCode,
        String productName,
        BigDecimal requiredQty,
        BigDecimal onHandQty,
        BigDecimal shortageQty,
        PlannedOrderStatus status,
        Long sourceSalesOrderId,
        String sourceSalesOrderNumber,
        String convertedProductionNumber,
        LocalDate orderDate,
        LocalDateTime createdAt,
        String createdBy
) {}
