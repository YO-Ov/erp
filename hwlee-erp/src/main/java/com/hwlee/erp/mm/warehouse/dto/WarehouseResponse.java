package com.hwlee.erp.mm.warehouse.dto;

import com.hwlee.erp.common.entity.MasterStatus;
import java.time.LocalDateTime;

public record WarehouseResponse(
        Long id,
        String code,
        String name,
        String address,
        MasterStatus status,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
