package com.hwlee.erp.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 손익계산서(미니) 응답 — 기간 손익 요약 + 계정별 명세.
 *
 * <pre>
 * 매출총이익 = 매출 − 매출원가
 * 영업이익   = 매출총이익 − 판매관리비
 * 당기순이익 = 수익총계 − 비용총계   (영업외 항목 없는 미니 버전이라 영업이익과 동일)
 * </pre>
 */
public record IncomeStatementResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal revenue,
        BigDecimal costOfGoodsSold,
        BigDecimal grossProfit,
        BigDecimal sgaExpense,
        BigDecimal operatingProfit,
        BigDecimal netIncome,
        List<IncomeStatementLine> revenueLines,
        List<IncomeStatementLine> expenseLines) {
}
