package com.hwlee.erp.report;

import com.hwlee.erp.report.dto.IncomeStatementResponse;
import com.hwlee.erp.report.dto.InventoryReportResponse;
import com.hwlee.erp.report.dto.SalesReportResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 리포트 REST — 매출 / 재고 현황 / 손익계산서. 조회 전용(FINANCE·ADMIN).
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
public class ReportController {

    private final ReportService service;

    /** 매출 리포트. unit=DAY(기본)/MONTH. */
    @GetMapping("/sales")
    public SalesReportResponse sales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "DAY") String unit) {
        return service.salesReport(from, to, unit);
    }

    /** 재고 현황 리포트. itemId/warehouseId 생략 시 전체. */
    @GetMapping("/inventory")
    public InventoryReportResponse inventory(
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) Long warehouseId) {
        return service.inventoryReport(itemId, warehouseId);
    }

    /** 손익계산서(미니). */
    @GetMapping("/income-statement")
    public IncomeStatementResponse incomeStatement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.incomeStatement(from, to);
    }
}
