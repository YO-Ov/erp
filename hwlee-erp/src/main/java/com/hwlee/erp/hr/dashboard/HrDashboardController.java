package com.hwlee.erp.hr.dashboard;

import com.hwlee.erp.hr.dashboard.dto.HrDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 인사(HR) 대시보드 집계 API — 인사/관리자만. */
@RestController
@RequestMapping("/api/hr/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR','ADMIN')")
public class HrDashboardController {

    private final HrDashboardService service;

    @GetMapping
    public HrDashboardResponse summary() {
        return service.summary();
    }
}
