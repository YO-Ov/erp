package com.hwlee.erp.master.item.dto;

import com.hwlee.erp.common.entity.MasterStatus;
import com.hwlee.erp.master.item.ItemType;
import com.hwlee.erp.master.item.ItemUnit;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ItemResponse(
        Long id,
        String code,
        String name,
        String category,
        ItemType itemType,
        ItemUnit unit,
        BigDecimal standardCost,
        BigDecimal standardPrice,
        MasterStatus status,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
