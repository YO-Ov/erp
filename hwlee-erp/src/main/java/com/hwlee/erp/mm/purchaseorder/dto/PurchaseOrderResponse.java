package com.hwlee.erp.mm.purchaseorder.dto;

import com.hwlee.erp.mm.purchaseorder.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderResponse(
        Long id,
        String number,
        Long vendorId,
        String vendorCode,
        String vendorName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        PurchaseOrderStatus status,
        LocalDate orderDate,
        LocalDate expectedDate,
        String remark,
        BigDecimal totalAmount,
        List<PurchaseOrderLineResponse> lines,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
