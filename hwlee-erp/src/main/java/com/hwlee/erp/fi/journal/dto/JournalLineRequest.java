package com.hwlee.erp.fi.journal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * 수동 전표 라인 입력. 차변 또는 대변 중 한 쪽만 > 0 이어야 한다 — 서비스가 검증.
 */
public record JournalLineRequest(
        @NotBlank String accountCode,
        @NotNull @PositiveOrZero BigDecimal debit,
        @NotNull @PositiveOrZero BigDecimal credit
) {}
