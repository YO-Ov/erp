package com.hwlee.erp.report.dto;

import java.math.BigDecimal;

/**
 * 매출 리포트 한 행 — 기간(일자 또는 월) 단위 집계.
 *
 * @param period       집계 구간 라벨(일별 yyyy-MM-dd, 월별 yyyy-MM, 합계행은 "합계")
 * @param invoiceCount 인보이스 건수
 * @param subtotal     공급가 합계
 * @param taxAmount    부가세 합계
 * @param totalAmount  합계(공급가+세액)
 */
public record SalesReportRow(
        String period,
        Long invoiceCount,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal totalAmount) {
}
