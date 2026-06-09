package com.hwlee.erp.report.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 매출 리포트 응답 — 구간 목록 + 합계행.
 *
 * @param unit  집계 단위("DAY" 또는 "MONTH")
 * @param from  조회 시작일
 * @param to    조회 종료일
 * @param rows  구간별 집계
 * @param total 전체 합계(period="합계")
 */
public record SalesReportResponse(
        String unit,
        LocalDate from,
        LocalDate to,
        List<SalesReportRow> rows,
        SalesReportRow total) {
}
