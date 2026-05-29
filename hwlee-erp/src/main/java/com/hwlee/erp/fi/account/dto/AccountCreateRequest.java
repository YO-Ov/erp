package com.hwlee.erp.fi.account.dto;

import com.hwlee.erp.fi.account.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccountCreateRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull AccountType type,
        String parentCode,        // null 이면 루트 계정
        @NotNull Boolean postable // 헤더 계정이면 false
) {}
