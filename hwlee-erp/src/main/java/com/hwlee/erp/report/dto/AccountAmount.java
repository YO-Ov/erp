package com.hwlee.erp.report.dto;

import com.hwlee.erp.fi.account.AccountType;
import java.math.BigDecimal;

/**
 * 손익계산서 집계 중간 결과 — 계정별 차변/대변 합(POSTED 전표 기준).
 */
public record AccountAmount(
        String code,
        String name,
        AccountType type,
        BigDecimal debitSum,
        BigDecimal creditSum) {

    /**
     * 정상방향 기준 순액. 수익(REVENUE)은 대변−차변, 비용(EXPENSE)은 차변−대변.
     */
    public BigDecimal normalAmount() {
        return switch (type) {
            case REVENUE -> creditSum.subtract(debitSum);
            default -> debitSum.subtract(creditSum); // EXPENSE 등
        };
    }
}
