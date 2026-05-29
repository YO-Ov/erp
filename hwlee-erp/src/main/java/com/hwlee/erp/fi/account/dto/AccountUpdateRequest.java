package com.hwlee.erp.fi.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccountUpdateRequest(
        @NotBlank String name,
        String parentCode,
        @NotNull Boolean postable
) {}
