package com.hwlee.erp.master.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DepartmentCreateRequest(
        @NotBlank @Size(max = 30) @Pattern(regexp = "^DEPT-[A-Z0-9_-]+$",
                message = "code 는 DEPT-XXX 형식이어야 한다") String code,
        @NotBlank @Size(max = 100) String name,
        String parentCode
) {}
