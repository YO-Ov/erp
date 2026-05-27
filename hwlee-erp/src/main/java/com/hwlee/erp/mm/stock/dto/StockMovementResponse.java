package com.hwlee.erp.mm.stock.dto;

import com.hwlee.erp.mm.stock.MovementReason;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockMovementResponse(
        Long id,
        Long itemId,
        String itemCode,
        String itemName,
        Long warehouseId,
        String warehouseCode,
        BigDecimal qtyDelta,
        BigDecimal unitCost,
        MovementReason reason,
        String refType,
        Long refId,
        LocalDateTime movedAt
) {}
