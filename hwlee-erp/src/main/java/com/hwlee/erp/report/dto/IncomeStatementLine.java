package com.hwlee.erp.report.dto;

import java.math.BigDecimal;

/**
 * 손익계산서 명세 한 줄 — 계정 단위 금액(정상방향 순액).
 */
public record IncomeStatementLine(
        String accountCode,
        String accountName,
        BigDecimal amount) {
}
