package com.hwlee.erp.sd.dashboard;

import com.hwlee.erp.sd.dashboard.dto.SdDashboardResponse;
import com.hwlee.erp.sd.dashboard.dto.SdDashboardResponse.RecentOrder;
import com.hwlee.erp.sd.order.SalesOrderRepository;
import com.hwlee.erp.sd.order.SalesOrderStatus;
import com.hwlee.erp.sd.quotation.QuotationRepository;
import com.hwlee.erp.sd.quotation.QuotationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 영업 대시보드 집계 — 수주/견적을 상태·기간별로 서버에서 정확히 합산한다. */
@Service
@RequiredArgsConstructor
public class SalesDashboardService {

    private final SalesOrderRepository orderRepo;
    private final QuotationRepository quotationRepo;

    @Transactional(readOnly = true)
    public SdDashboardResponse summary() {
        YearMonth ym = YearMonth.now();
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        long monthCount = orderRepo.countByOrderDateBetween(from, to);
        BigDecimal monthAmount = orderRepo.sumAmountByOrderDateBetween(from, to);

        // 상태별 건수(파이프라인) + 출하대기(CONFIRMED)/미청구(SHIPPED) 금액.
        Map<String, Long> pipeline = new LinkedHashMap<>();
        for (SalesOrderStatus s : SalesOrderStatus.values()) {
            pipeline.put(s.name(), 0L);
        }
        long shipCnt = 0, uninvCnt = 0;
        BigDecimal shipAmt = BigDecimal.ZERO, uninvAmt = BigDecimal.ZERO;
        for (var row : orderRepo.aggregateByStatus()) {
            pipeline.put(row.getStatus().name(), row.getCount());
            if (row.getStatus() == SalesOrderStatus.CONFIRMED) {
                shipCnt = row.getCount();
                shipAmt = row.getAmount();
            } else if (row.getStatus() == SalesOrderStatus.SHIPPED) {
                uninvCnt = row.getCount();
                uninvAmt = row.getAmount();
            }
        }

        long quotationToSend = quotationRepo.countByStatus(QuotationStatus.APPROVED);

        List<RecentOrder> recent = orderRepo.findTop5ByOrderByOrderDateDescIdDesc().stream()
                .map(o -> new RecentOrder(o.getNumber(), o.getCustomer().getName(),
                        o.getTotalAmount(), o.getStatus().name(), o.getOrderDate()))
                .toList();

        return new SdDashboardResponse(monthCount, monthAmount, shipCnt, shipAmt,
                uninvCnt, uninvAmt, quotationToSend, pipeline, recent);
    }

    /**
     * 상태별 수주 건수(파이프라인) — 기간 필터 버전. 대시보드 '수주 진행 현황' 차트가 쓴다.
     *
     * @param from 수주일 시작(포함). null 이면 전체 기간.
     * @param to   수주일 종료(포함). null 이면 전체 기간.
     * @return 모든 상태를 0 으로 초기화한 뒤 채운, 상태명→건수 맵(상태 enum 정의 순서 유지).
     */
    @Transactional(readOnly = true)
    public Map<String, Long> orderStatus(LocalDate from, LocalDate to) {
        Map<String, Long> pipeline = new LinkedHashMap<>();
        for (SalesOrderStatus s : SalesOrderStatus.values()) {
            pipeline.put(s.name(), 0L);
        }
        var rows = (from != null && to != null)
                ? orderRepo.aggregateByStatusBetween(from, to)
                : orderRepo.aggregateByStatus();
        for (var row : rows) {
            pipeline.put(row.getStatus().name(), row.getCount());
        }
        return pipeline;
    }
}
