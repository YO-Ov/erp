package com.hwlee.erp.pp.order.dto;

import com.hwlee.erp.pp.order.ProductionOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProductionOrderResponse(
        Long id,
        String number,
        Long productItemId,
        String productCode,
        String productName,
        Long warehouseId,
        String warehouseName,
        BigDecimal quantity,
        ProductionOrderStatus status,
        LocalDate orderDate,
        LocalDate dueDate,
        LocalDateTime completedAt,
        List<ProductionOrderLineResponse> lines,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
