package com.hwlee.erp.fi.dashboard;

import com.hwlee.erp.fi.dashboard.dto.FiDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 재무(FI) 대시보드 집계 API — 재무/관리자만. */
@RestController
@RequestMapping("/api/fi/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
public class FiDashboardController {

    private final FiDashboardService service;

    @GetMapping
    public FiDashboardResponse summary() {
        return service.summary();
    }
}
