package com.hwlee.erp.pp.dashboard;

import com.hwlee.erp.pp.dashboard.dto.PpDashboardResponse;
import com.hwlee.erp.pp.dashboard.dto.PpDashboardResponse.RecentOrder;
import com.hwlee.erp.pp.order.ProductionOrderRepository;
import com.hwlee.erp.pp.order.ProductionOrderStatus;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 생산 대시보드 집계 — 생산지시를 상태·기간별로 서버에서 정확히 합산한다. */
@Service
@RequiredArgsConstructor
public class PpDashboardService {

    private final ProductionOrderRepository orderRepo;

    @Transactional(readOnly = true)
    public PpDashboardResponse summary() {
        YearMonth ym = YearMonth.now();
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        long thisMonthCount = orderRepo.countByOrderDateBetween(from, to);

        // 상태별 건수(파이프라인) — 모든 상태 0 초기화 후 채움(enum 정의 순서 유지).
        Map<String, Long> pipeline = new LinkedHashMap<>();
        for (ProductionOrderStatus s : ProductionOrderStatus.values()) {
            pipeline.put(s.name(), 0L);
        }
        for (var row : orderRepo.aggregateByStatus()) {
            pipeline.put(row.getStatus().name(), row.getCount());
        }
        long inProgress = pipeline.get(ProductionOrderStatus.RELEASED.name());      // 진행중(착수됨)
        long awaiting = pipeline.get(ProductionOrderStatus.PLANNED.name());         // 완료 대기(착수 예정)

        List<RecentOrder> recent = orderRepo.findTop5ByOrderByOrderDateDescIdDesc().stream()
                .map(o -> new RecentOrder(o.getNumber(), o.getProduct().getName(),
                        o.getQuantity(), o.getStatus().name(), o.getOrderDate()))
                .toList();

        return new PpDashboardResponse(inProgress, awaiting, thisMonthCount, pipeline, recent);
    }
}
