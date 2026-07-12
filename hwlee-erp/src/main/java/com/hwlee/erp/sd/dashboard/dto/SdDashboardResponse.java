package com.hwlee.erp.sd.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 영업(SD) 대시보드 집계 응답.
 *
 * <p>목록 API 를 클라이언트에서 합산하면 페이징 때문에 부정확하므로, 서버에서 정확히 집계해 내려준다.
 */
public record SdDashboardResponse(
        long thisMonthOrderCount,        // 이번 달 수주 건수
        BigDecimal thisMonthOrderAmount, // 이번 달 수주 금액
        long awaitingShipmentCount,      // 출하 대기(CONFIRMED) 건수
        BigDecimal awaitingShipmentAmount,
        long uninvoicedCount,            // 미청구(SHIPPED) 건수
        BigDecimal uninvoicedAmount,
        long quotationToSendCount,       // 견적 발송 대기(APPROVED)
        Map<String, Long> pipeline,      // 상태명 → 건수
        List<RecentOrder> recentOrders) {

    public record RecentOrder(
            String number,
            String customerName,
            BigDecimal totalAmount,
            String status,
            LocalDate orderDate) {
    }
}
