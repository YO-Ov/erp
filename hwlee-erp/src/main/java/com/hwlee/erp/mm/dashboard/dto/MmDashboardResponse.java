package com.hwlee.erp.mm.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 구매(MM) 대시보드 집계 응답.
 *
 * <p>목록 API 를 클라이언트에서 합산하면 페이징 때문에 부정확하므로, 서버에서 정확히 집계해 내려준다.
 * 발주 금액은 저장 컬럼이 아니라 발주 라인 합(PurchaseOrderLine.lineTotal)이다.
 *
 * <p>입고 대기(awaitingReceipt) = 발주 확정(CONFIRMED) 상태. CONFIRMED 는 거래처에 발주됐으나
 * 아직 전량 입고 전(입고 대기·부분 입고 포함)을 뜻하며, 전량 입고되면 RECEIVED 로 자동 전이한다.
 */
public record MmDashboardResponse(
        long thisMonthOrderCount,          // 이번 달 발주 건수(발주일 기준)
        BigDecimal thisMonthOrderAmount,   // 이번 달 발주 금액
        long awaitingReceiptCount,         // 입고 대기(CONFIRMED) 건수
        BigDecimal awaitingReceiptAmount,  // 입고 대기(CONFIRMED) 금액
        Map<String, Long> pipeline,        // 상태명 → 건수 (enum 정의 순서 유지)
        List<RecentPurchaseOrder> recentOrders) {

    public record RecentPurchaseOrder(
            String number,
            String vendorName,
            BigDecimal totalAmount,
            String status,
            LocalDate orderDate) {
    }
}
