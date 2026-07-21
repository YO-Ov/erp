package com.hwlee.erp.pp.dashboard;

import com.hwlee.erp.pp.dashboard.dto.PpDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 생산(PP) 대시보드 집계 API — 생산/관리자만. */
@RestController
@RequestMapping("/api/pp/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCTION','ADMIN')")
public class PpDashboardController {

    private final PpDashboardService service;

    @GetMapping
    public PpDashboardResponse summary() {
        return service.summary();
    }
}
