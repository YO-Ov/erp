package com.hwlee.erp.pp.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 생산(PP) 대시보드 집계 응답.
 *
 * <p>목록 API 를 클라이언트에서 합산하면 페이징 때문에 부정확하므로, 서버에서 정확히 집계해 내려준다.
 */
public record PpDashboardResponse(
        long inProgressCount,          // 진행중 생산지시(RELEASED) 건수
        long awaitingCompletionCount,  // 완료 대기 = 착수 예정(PLANNED) 건수
        long thisMonthOrderCount,      // 이번 달 생산지시 건수(지시일 기준)
        Map<String, Long> pipeline,    // 상태명 → 건수
        List<RecentOrder> recentOrders) {

    public record RecentOrder(
            String number,
            String productName,
            BigDecimal quantity,
            String status,
            LocalDate orderDate) {
    }
}
