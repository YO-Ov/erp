package com.hwlee.erp.pp.integration.mes;

import java.time.LocalDateTime;

/**
 * MES 작업지시 전송 결과 응답.
 */
public record DispatchResult(
        String erpOrderNo,
        String mesWorkOrderNo,
        String mesStatus,
        LocalDateTime dispatchedAt) {
}
