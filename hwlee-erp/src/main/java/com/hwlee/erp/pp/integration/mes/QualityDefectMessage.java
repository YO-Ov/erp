package com.hwlee.erp.pp.integration.mes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MES → ERP 불량 발생 이벤트(Kafka 수신). MES 의 QualityDefectEvent 와 동일 구조.
 */
public record QualityDefectMessage(
        String eventId,
        String workOrderNo,
        String erpOrderNo,
        String productCode,
        BigDecimal defectQty,
        String defectReasonCode,
        String defectReasonName,
        LocalDateTime inspectedAt) {
}
