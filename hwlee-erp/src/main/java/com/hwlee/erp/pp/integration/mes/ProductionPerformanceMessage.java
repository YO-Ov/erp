package com.hwlee.erp.pp.integration.mes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MES → ERP 생산 실적 이벤트(Kafka 수신). MES 의 ProductionPerformanceEvent 와 동일 구조.
 * (계약: contracts/events/mes-production-performance.json)
 */
public record ProductionPerformanceMessage(
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
