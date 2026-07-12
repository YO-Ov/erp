package com.hwlee.erp.sd.dashboard;

import com.hwlee.erp.sd.dashboard.dto.SdDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
