package com.hwlee.mes.quality;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwlee.mes.integration.event.QualityDefectEvent;
import com.hwlee.mes.integration.outbox.OutboxEvent;
import com.hwlee.mes.integration.outbox.OutboxEventRepository;
import com.hwlee.mes.quality.dto.InspectRequest;
import com.hwlee.mes.quality.dto.QualityInspectionResponse;
import com.hwlee.mes.workorder.WorkOrder;
import com.hwlee.mes.workorder.WorkOrderRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 품질 검사 — 합격/불량 판정. 불량이면 같은 트랜잭션에서 Outbox 에 불량 이벤트를 적재해 ERP 로 통보한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityService {

    private static final String AGGREGATE_TYPE = "QUALITY_INSPECTION";
    private static final String EVENT_TYPE = "QUALITY_DEFECT";

    private final WorkOrderRepository workOrderRepository;
    private final DefectReasonRepository defectReasonRepository;
    private final QualityInspectionRepository inspectionRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public QualityInspectionResponse inspect(Long workOrderId, InspectRequest req) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("작업지시를 찾을 수 없습니다: " + workOrderId));

        BigDecimal defect = (req.defectQty() == null) ? BigDecimal.ZERO : req.defectQty();
        DefectReason reason = null;
        if (defect.signum() > 0) {
            if (req.defectReasonId() == null) {
                throw new IllegalArgumentException("불량이 있으면 불량 사유는 필수입니다.");
            }
            reason = defectReasonRepository.findById(req.defectReasonId())
                    .orElseThrow(() -> new IllegalArgumentException("불량 사유를 찾을 수 없습니다: " + req.defectReasonId()));
        }

        QualityInspection qi = QualityInspection.of(
                wo, req.inspectedQty(), req.passedQty(), defect, reason, LocalDateTime.now(), req.note());
        inspectionRepository.save(qi);

        if (qi.hasDefect()) {
            appendDefectOutbox(wo, qi);
        }
        return QualityInspectionResponse.from(qi);
    }

    @Transactional(readOnly = true)
    public List<QualityInspectionResponse> list(Long workOrderId) {
        return inspectionRepository.findByWorkOrderIdOrderByIdAsc(workOrderId).stream()
                .map(QualityInspectionResponse::from)
                .toList();
    }

    private void appendDefectOutbox(WorkOrder wo, QualityInspection qi) {
        String eventId = "QI-" + qi.getId();
        QualityDefectEvent event = new QualityDefectEvent(
                eventId, wo.getWorkOrderNo(), wo.getErpOrderNo(), wo.getProductCode(),
                qi.getDefectQty(),
                qi.getDefectReason() != null ? qi.getDefectReason().getCode() : null,
                qi.getDefectReason() != null ? qi.getDefectReason().getName() : null,
                qi.getInspectedAt());
        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(OutboxEvent.pending(
                    AGGREGATE_TYPE, wo.getWorkOrderNo(), EVENT_TYPE, eventId, payload));
            log.info("[품질] 불량 이벤트 Outbox 적재 eventId={}", eventId);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("불량 이벤트 직렬화 실패: " + eventId, e);
        }
    }
}
