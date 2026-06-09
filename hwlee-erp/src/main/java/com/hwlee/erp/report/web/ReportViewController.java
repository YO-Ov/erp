package com.hwlee.erp.report.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 리포트 화면(껍데기) 라우팅 — 실제 데이터는 JS 가 {@code /api/reports/*} 를 호출해 채운다.
 */
@Controller
@PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
public class ReportViewController {

    @GetMapping("/reports/sales")
    public String salesReport() {
        return "report/sales";
    }

    @GetMapping("/reports/inventory")
    public String inventoryReport() {
        return "report/inventory";
    }

    @GetMapping("/reports/income-statement")
    public String incomeStatement() {
        return "report/income-statement";
    }
}
