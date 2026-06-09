package com.hwlee.erp.pp.integration.mes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ERP↔MES 정합성 검증 결과 — 분산 시스템의 사후 일관성 점검.
 */
public record ReconciliationResponse(
        LocalDateTime checkedAt,
        int totalChecked,
        boolean consistent,
        List<Discrepancy> discrepancies) {

    /**
     * @param type MISSING_IN_MES(ERP 전송했으나 MES에 없음) / QTY_MISMATCH(수량 불일치)
     */
    public record Discrepancy(String erpOrderNo, String type, String detail) {
    }
}
