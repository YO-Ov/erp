package com.hwlee.mes.performance.dto;

import com.hwlee.mes.performance.ProductionResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 생산 실적 응답(자재 투입 포함).
 */
public record ProductionResultResponse(
        Long id,
        int seq,
        BigDecimal goodQty,
        BigDecimal defectQty,
        LocalDateTime reportedAt,
        String defectReasonCode,
        String defectReasonName,
        List<ConsumptionResponse> consumptions) {

    public record ConsumptionResponse(String componentCode, String componentName, BigDecimal consumedQty) {
    }

    public static ProductionResultResponse from(ProductionResult r) {
        List<ConsumptionResponse> cons = r.getConsumptions().stream()
                .map(c -> new ConsumptionResponse(c.getComponentCode(), c.getComponentName(), c.getConsumedQty()))
                .toList();
        return new ProductionResultResponse(r.getId(), r.getSeq(), r.getGoodQty(), r.getDefectQty(),
                r.getReportedAt(),
                r.getDefectReason() != null ? r.getDefectReason().getCode() : null,
                r.getDefectReason() != null ? r.getDefectReason().getName() : null,
                cons);
    }
}
