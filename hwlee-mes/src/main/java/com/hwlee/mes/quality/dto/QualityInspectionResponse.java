package com.hwlee.mes.quality.dto;

import com.hwlee.mes.quality.QualityInspection;
import com.hwlee.mes.quality.QualityResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QualityInspectionResponse(
        Long id,
        String workOrderNo,
        BigDecimal inspectedQty,
        BigDecimal passedQty,
        BigDecimal defectQty,
        String defectReasonCode,
        String defectReasonName,
        QualityResult result,
        LocalDateTime inspectedAt,
        String note) {

    public static QualityInspectionResponse from(QualityInspection qi) {
        return new QualityInspectionResponse(
                qi.getId(),
                qi.getWorkOrder().getWorkOrderNo(),
                qi.getInspectedQty(), qi.getPassedQty(), qi.getDefectQty(),
                qi.getDefectReason() != null ? qi.getDefectReason().getCode() : null,
                qi.getDefectReason() != null ? qi.getDefectReason().getName() : null,
                qi.getResult(), qi.getInspectedAt(), qi.getNote());
    }
}
