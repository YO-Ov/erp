package com.hwlee.erp.fi.dashboard.dto;

import java.math.BigDecimal;

/**
 * 재무(FI) 대시보드 집계 응답.
 *
 * <p>목록 API 를 클라이언트에서 합산하면 페이징 때문에 부정확하므로, 서버에서 정확히 집계해 내려준다.
 *
 * <p>각 지표 정의:
 * <ul>
 *   <li>{@code thisMonthSalesAmount} — 이번 달({@code YearMonth.now()}) 발행(ISSUED)된 인보이스
 *       총액(부가세 포함) 합. 매출 인식 기준.</li>
 *   <li>{@code thisMonthReceiptAmount} — 이번 달 확정(POSTED)된 입금(RECEIPT) 금액 합. 실제 수금액.</li>
 *   <li>{@code accountsReceivable} — 미수금 = (발행된 모든 ISSUED 인보이스 총액 합) − (확정된 모든
 *       POSTED 입금 합). 여신 미수금 산정과 동일한 정의(발행 인보이스 − 수금). 0 미만이면 0 으로 절사.</li>
 *   <li>{@code pendingJournalCount} — 승인(확정) 대기 전표 건수 = 상태 DRAFT 인 JournalEntry 건수.</li>
 *   <li>{@code pendingCreditRequestCount} — 승인 대기 여신 요청 건수 = 상태 PENDING 인
 *       CreditLimitRequest 건수.</li>
 *   <li>{@code accountsReceivableDefinition} — 미수금 계산 정의 문자열(프론트 툴팁/주석용).</li>
 * </ul>
 */
public record FiDashboardResponse(
        BigDecimal thisMonthSalesAmount,     // 이번 달 매출(발행 인보이스 총액 합)
        BigDecimal thisMonthReceiptAmount,   // 이번 달 입금(확정 수금 합)
        BigDecimal accountsReceivable,       // 미수금 = 발행 인보이스 합 − 확정 입금 합
        long pendingJournalCount,            // 승인 대기 전표(DRAFT) 건수
        long pendingCreditRequestCount,      // 여신 요청 대기(PENDING) 건수
        String accountsReceivableDefinition) {
}
