package com.hwlee.erp.mm.dashboard;

import com.hwlee.erp.mm.dashboard.dto.MmDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 구매(MM) 대시보드 집계 API — 구매/관리자만. */
@RestController
@RequestMapping("/api/mm/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PURCHASING','ADMIN')")
public class MmDashboardController {

    private final MmDashboardService service;

    @GetMapping
    public MmDashboardResponse summary() {
        return service.summary();
    }
}
