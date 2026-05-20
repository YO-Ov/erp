package com.hwlee.erp.master.department.dto;

import com.hwlee.erp.common.entity.MasterStatus;
import java.time.LocalDateTime;

public record DepartmentResponse(
        Long id,
        String code,
        String name,
        String parentCode,
        MasterStatus status,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
