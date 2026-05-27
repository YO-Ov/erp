package com.hwlee.erp.mm.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WarehouseCreateRequest(
        @NotBlank @Size(max = 30) @Pattern(regexp = "^WH-[A-Z0-9_-]+$",
                message = "code 는 WH-XXX 형식이어야 한다") String code,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) String address
) {}
