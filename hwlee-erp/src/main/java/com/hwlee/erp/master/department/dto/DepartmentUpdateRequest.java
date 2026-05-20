package com.hwlee.erp.master.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        String parentCode
) {}
