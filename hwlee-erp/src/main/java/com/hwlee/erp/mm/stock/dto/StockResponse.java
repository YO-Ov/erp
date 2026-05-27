package com.hwlee.erp.mm.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockResponse(
        Long id,
        Long itemId,
        String itemCode,
        String itemName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        BigDecimal qtyOnHand,
        BigDecimal averageCost,
        Long version,
        LocalDateTime updatedAt
) {}
