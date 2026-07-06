package com.hwlee.erp.master.vendoritem.dto;

import com.hwlee.erp.common.entity.MasterStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VendorItemResponse(
        Long id,
        Long vendorId,
        String vendorCode,
        String vendorName,
        Long itemId,
        String itemCode,
        String itemName,
        BigDecimal supplyPrice,
        int leadTimeDays,
        MasterStatus status,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
