package com.hwlee.mes.integration.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MES → ERP 불량 발생 이벤트(Kafka). 토픽: mes.quality.defect.
 * ERP 는 재고/회계에 반영하지 않고 통계로 기록만 한다(품질 상세는 MES 소유).
 */
public record QualityDefectEvent(
        String eventId,
        String workOrderNo,
        String erpOrderNo,
        String productCode,
        BigDecimal defectQty,
        String defectReasonCode,
        String defectReasonName,
        LocalDateTime inspectedAt) {
}
