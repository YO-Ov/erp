package com.hwlee.erp.fi.credit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** 여신 상향 요청 생성 (영업). */
public record CreditLimitRequestCreateRequest(
        @NotNull Long customerId,
        @NotNull @Positive BigDecimal requestedLimit,
        @NotBlank String reason
) {}
