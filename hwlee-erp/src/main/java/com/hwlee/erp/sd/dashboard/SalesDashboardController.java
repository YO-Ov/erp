package com.hwlee.erp.sd.dashboard;

import com.hwlee.erp.sd.dashboard.dto.SdDashboardResponse;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 영업 대시보드 집계 API — 영업/관리자만. */
@RestController
@RequestMapping("/api/sd/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES','ADMIN')")
public class SalesDashboardController {

    private final SalesDashboardService service;

    @GetMapping
    public SdDashboardResponse summary() {
        return service.summary();
    }

    /**
     * 상태별 수주 건수(파이프라인) — 기간 필터. 대시보드 '수주 진행 현황' 차트용.
     * dateFrom·dateTo 를 둘 다 주면 그 기간, 없으면 전체 기간.
     */
    @GetMapping("/order-status")
    public Map<String, Long> orderStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return service.orderStatus(dateFrom, dateTo);
    }
}
