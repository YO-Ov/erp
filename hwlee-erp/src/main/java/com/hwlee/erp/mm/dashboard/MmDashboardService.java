package com.hwlee.erp.mm.dashboard;

import com.hwlee.erp.mm.dashboard.dto.MmDashboardResponse;
import com.hwlee.erp.mm.dashboard.dto.MmDashboardResponse.RecentPurchaseOrder;
import com.hwlee.erp.mm.purchaseorder.PurchaseOrderRepository;
import com.hwlee.erp.mm.purchaseorder.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 구매(MM) 대시보드 집계 — 발주를 상태·기간별로 서버에서 정확히 합산한다. */
@Service
@RequiredArgsConstructor
public class MmDashboardService {

    private final PurchaseOrderRepository orderRepo;

    @Transactional(readOnly = true)
    public MmDashboardResponse summary() {
        YearMonth ym = YearMonth.now();
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        long monthCount = orderRepo.countByOrderDateBetween(from, to);
        BigDecimal monthAmount = orderRepo.sumAmountByOrderDateBetween(from, to);

        // 상태별 건수(파이프라인) + 입고 대기(CONFIRMED) 금액.
        Map<String, Long> pipeline = new LinkedHashMap<>();
        for (PurchaseOrderStatus s : PurchaseOrderStatus.values()) {
            pipeline.put(s.name(), 0L);
        }
        long awaitingCnt = 0;
        BigDecimal awaitingAmt = BigDecimal.ZERO;
        for (var row : orderRepo.aggregateByStatus()) {
            pipeline.put(row.getStatus().name(), row.getCount());
            if (row.getStatus() == PurchaseOrderStatus.CONFIRMED) {
                awaitingCnt = row.getCount();
                awaitingAmt = row.getAmount();
            }
        }

        List<RecentPurchaseOrder> recent = orderRepo.findTop5ByOrderByOrderDateDescIdDesc().stream()
                .map(o -> new RecentPurchaseOrder(o.getNumber(), o.getVendor().getName(),
                        o.totalAmount(), o.getStatus().name(), o.getOrderDate()))
                .toList();

        return new MmDashboardResponse(monthCount, monthAmount, awaitingCnt, awaitingAmt,
                pipeline, recent);
    }
}
