package com.hwlee.erp.sd.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 기간별 수주 집계 응답 — "이번 달 수주 합계" 류 질의에 답하기 위한 것.
 *
 * <p>목록 API 를 클라이언트에서 합산하면 페이징 때문에 부정확하므로, 서버에서 정확히 집계해 내려준다.
 * ({@link com.hwlee.erp.sd.dashboard.dto.SdDashboardResponse} 와 같은 이유 — 그쪽은 기간이
 * '이번 달' 로 고정이라, 임의 기간(지난달·분기·연간)은 이 API 를 쓴다.)
 *
 * <p>'수주 합계' 는 건수인지 금액인지 중의적이므로 둘 다 내려준다.
 */
public record SalesOrderSummaryResponse(
        LocalDate dateFrom,
        LocalDate dateTo,
        long orderCount,      // 기간 내 수주 건수
        BigDecimal totalAmount // 기간 내 수주 금액 합계
) {}
