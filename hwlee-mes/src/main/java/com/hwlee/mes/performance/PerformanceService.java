package com.hwlee.mes.performance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwlee.mes.integration.event.ProductionPerformanceEvent;
import com.hwlee.mes.integration.outbox.OutboxEvent;
import com.hwlee.mes.integration.outbox.OutboxEventRepository;
import com.hwlee.mes.master.equipment.Equipment;
import com.hwlee.mes.master.equipment.EquipmentRepository;
import com.hwlee.mes.master.operator.Operator;
import com.hwlee.mes.master.operator.OperatorRepository;
import com.hwlee.mes.performance.dto.ProductionResultResponse;
import com.hwlee.mes.performance.dto.ReportRequest;
import com.hwlee.mes.workorder.WorkOrder;
import com.hwlee.mes.workorder.WorkOrderLine;
import com.hwlee.mes.workorder.WorkOrderRepository;
import com.hwlee.mes.workorder.dto.WorkOrderResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현장 실행 서비스 — 작업 시작/일시정지/재개/완료 + 생산 실적 보고(자재 투입 자동 기록).
 */
@Service
@RequiredArgsConstructor
public class PerformanceService {

    private static final String AGGREGATE_TYPE = "WORK_ORDER";
    private static final String EVENT_TYPE = "PRODUCTION_PERFORMANCE";

    private final WorkOrderRepository workOrderRepository;
    private final EquipmentRepository equipmentRepository;
    private final OperatorRepository operatorRepository;
    private final ProductionResultRepository resultRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public WorkOrderResponse start(Long workOrderId, Long equipmentId, Long operatorId) {
        WorkOrder wo = find(workOrderId);
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("설비를 찾을 수 없습니다: " + equipmentId));
        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new IllegalArgumentException("작업자를 찾을 수 없습니다: " + operatorId));
        wo.start(equipment, operator, LocalDateTime.now());
        return WorkOrderResponse.from(wo);
    }

    @Transactional
    public WorkOrderResponse pause(Long workOrderId) {
        WorkOrder wo = find(workOrderId);
        wo.pause();
        return WorkOrderResponse.from(wo);
    }

    @Transactional
    public WorkOrderResponse resume(Long workOrderId) {
        WorkOrder wo = find(workOrderId);
        wo.resume();
        return WorkOrderResponse.from(wo);
    }

    @Transactional
    public WorkOrderResponse complete(Long workOrderId) {
        WorkOrder wo = find(workOrderId);
        wo.complete(LocalDateTime.now());
        return WorkOrderResponse.from(wo);
    }

    /** 생산 실적 보고 — 양품/불량 누적 + BOM 비례 자재 투입 자동 기록. */
    @Transactional
    public ProductionResultResponse report(Long workOrderId, ReportRequest req) {
        WorkOrder wo = find(workOrderId);
        int seq = resultRepository.countByWorkOrderId(workOrderId) + 1;

        ProductionResult result = ProductionResult.of(
                wo, seq, req.goodQty(), req.defectQty(), LocalDateTime.now(), req.note());

        // 자재 투입 = 단위소요(라인소요 ÷ 지시수량) × 이번 양품수량.
        for (WorkOrderLine line : wo.getLines()) {
            BigDecimal unitPer = line.getRequiredQty()
                    .divide(wo.getQuantity(), 6, RoundingMode.HALF_UP);
            BigDecimal consumed = unitPer.multiply(req.goodQty()).setScale(4, RoundingMode.HALF_UP);
            result.addConsumption(line.getComponentCode(), line.getComponentName(), consumed);
        }

        wo.addProduction(req.goodQty(), req.defectQty());
        resultRepository.save(result);

        // ── Transactional Outbox: 실적 저장과 같은 트랜잭션에서 "보낼 메시지" 적재 ──
        appendOutbox(wo, result);

        return ProductionResultResponse.from(result);
    }

    /** 생산 실적 이벤트를 Outbox 에 적재(같은 트랜잭션). 발행은 Publisher 가 비동기로. */
    private void appendOutbox(WorkOrder wo, ProductionResult result) {
        List<ProductionPerformanceEvent.ConsumptionLine> consumptions = result.getConsumptions().stream()
                .map(c -> new ProductionPerformanceEvent.ConsumptionLine(
                        c.getComponentCode(), c.getComponentName(), c.getConsumedQty()))
                .toList();
        String eventId = wo.getWorkOrderNo() + "#" + result.getSeq();
        ProductionPerformanceEvent event = new ProductionPerformanceEvent(
                eventId, wo.getErpOrderNo(), wo.getWorkOrderNo(), result.getSeq(),
                result.getGoodQty(), result.getDefectQty(), consumptions, result.getReportedAt());
        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(OutboxEvent.pending(
                    AGGREGATE_TYPE, wo.getWorkOrderNo(), EVENT_TYPE, eventId, payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("실적 이벤트 직렬화 실패: " + eventId, e);
        }
    }

    @Transactional(readOnly = true)
    public List<ProductionResultResponse> results(Long workOrderId) {
        return resultRepository.findByWorkOrderIdWithConsumptions(workOrderId).stream()
                .map(ProductionResultResponse::from)
                .toList();
    }

    private WorkOrder find(Long workOrderId) {
        return workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("작업지시를 찾을 수 없습니다: " + workOrderId));
    }
}
