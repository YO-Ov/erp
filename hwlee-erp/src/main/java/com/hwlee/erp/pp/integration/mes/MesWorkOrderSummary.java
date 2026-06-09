package com.hwlee.erp.pp.integration.mes;

import java.math.BigDecimal;

/**
 * MES 작업지시 조회 응답(정합성 대조용 부분 필드). 알 수 없는 필드는 무시.
 */
public record MesWorkOrderSummary(
        String erpOrderNo,
        String workOrderNo,
        BigDecimal quantity,
        String status,
        BigDecimal producedQty) {
}
