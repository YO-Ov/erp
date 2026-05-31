package com.hwlee.erp.hr.payroll.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 급여대장 생성 요청 — 대상 월(YYYY-MM)만 받는다. 명세는 그 달 유효계약 + 근태로 자동 계산.
 */
public record PayrollRunCreateRequest(
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}", message = "period 는 YYYY-MM 형식이어야 합니다.")
        String period
) {}
