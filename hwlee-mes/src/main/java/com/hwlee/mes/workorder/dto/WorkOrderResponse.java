package com.hwlee.mes.workorder.dto;

import com.hwlee.mes.workorder.WorkOrder;
import com.hwlee.mes.workorder.WorkOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 작업지시 응답.
 */
public record WorkOrderResponse(
        Long id,
        String workOrderNo,
        String erpOrderNo,
        String productCode,
        String productName,
        BigDecimal quantity,
        LocalDate plannedDate,
        WorkOrderStatus status,
        LocalDateTime receivedAt,
        // Phase 13 — 현장 실행 상태
        BigDecimal producedQty,
        BigDecimal defectQty,
        String assignedEquipmentCode,
        String assignedOperatorCode,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        List<LineResponse> lines
) {
    public record LineResponse(
            int lineNo, String componentCode, String componentName, BigDecimal requiredQty, String unit) {
    }

    public static WorkOrderResponse from(WorkOrder w) {
        List<LineResponse> lines = w.getLines().stream()
                .map(l -> new LineResponse(l.getLineNo(), l.getComponentCode(), l.getComponentName(),
                        l.getRequiredQty(), l.getUnit()))
                .toList();
        return new WorkOrderResponse(w.getId(), w.getWorkOrderNo(), w.getErpOrderNo(),
                w.getProductCode(), w.getProductName(), w.getQuantity(), w.getPlannedDate(),
                w.getStatus(), w.getReceivedAt(),
                w.getProducedQty(), w.getDefectQty(),
                w.getAssignedEquipment() != null ? w.getAssignedEquipment().getCode() : null,
                w.getAssignedOperator() != null ? w.getAssignedOperator().getCode() : null,
                w.getStartedAt(), w.getFinishedAt(),
                lines);
    }
}
