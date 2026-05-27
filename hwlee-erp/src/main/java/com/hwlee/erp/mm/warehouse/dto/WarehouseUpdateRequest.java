package com.hwlee.erp.mm.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) String address
) {}
