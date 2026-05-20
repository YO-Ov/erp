package com.hwlee.erp.master.employee.dto;

import com.hwlee.erp.common.entity.MasterStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        String code,
        String name,
        String email,
        String departmentCode,
        String departmentName,
        LocalDate hireDate,
        MasterStatus status,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
