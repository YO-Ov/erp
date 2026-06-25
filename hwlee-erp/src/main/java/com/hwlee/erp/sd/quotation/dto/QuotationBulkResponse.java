package com.hwlee.erp.sd.quotation.dto;

import java.util.List;

/**
 * 일괄 작업 결과 — 요청 건수, 성공 건수, 그리고 실패한 건의 사유 목록.
 */
public record QuotationBulkResponse(
        int requested,
        int succeeded,
        List<Failure> failed
) {
    public record Failure(Long id, String number, String reason) {}
}
