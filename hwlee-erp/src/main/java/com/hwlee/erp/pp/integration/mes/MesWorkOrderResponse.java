package com.hwlee.erp.pp.integration.mes;

/**
 * MES 작업지시 수신 응답(필요 필드만). 알 수 없는 필드는 무시된다.
 */
public record MesWorkOrderResponse(
        Long id,
        String workOrderNo,
        String erpOrderNo,
        String status) {
}
