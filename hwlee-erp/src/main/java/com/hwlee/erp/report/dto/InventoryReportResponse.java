package com.hwlee.erp.report.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 재고 현황 리포트 응답 — 행 목록 + 평가액 총계.
 */
public record InventoryReportResponse(
        List<InventoryReportRow> rows,
        BigDecimal totalValuation) {
}
