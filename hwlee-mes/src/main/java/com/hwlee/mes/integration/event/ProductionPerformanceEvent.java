package com.hwlee.mes.integration.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MES → ERP 생산 실적 이벤트(Kafka payload). 계약: contracts/events/mes-production-performance.json
 *
 * @param eventId     멱등 키(수신측 중복 차단). workOrderNo#seq.
 */
public record ProductionPerformanceEvent(
        String eventId,
        String erpOrderNo,
        String workOrderNo,
        int seq,
        BigDecimal goodQty,
        BigDecimal defectQty,
        List<ConsumptionLine> consumptions,
        LocalDateTime reportedAt) {

    public record ConsumptionLine(String componentCode, String componentName, BigDecimal consumedQty) {
    }
}
