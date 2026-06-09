package com.hwlee.mes.workorder;

import com.hwlee.mes.workorder.dto.WorkOrderReceiveRequest;
import com.hwlee.mes.workorder.dto.WorkOrderResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 작업지시 수신 서비스 — 멱등 등록.
 *
 * <p>{@code erpOrderNo} 로 기존 작업지시를 먼저 찾아, 있으면 그대로 반환(중복 등록 안 함).
 * UNIQUE 제약이 최종 방어선이며, 동시 중복 요청도 한 건만 살아남는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository repository;

    /** @param created true=신규 등록, false=이미 존재(멱등) */
    public record ReceiveOutcome(WorkOrderResponse response, boolean created) {
    }

    @Transactional
    public ReceiveOutcome receive(WorkOrderReceiveRequest req) {
        return repository.findByErpOrderNo(req.erpOrderNo())
                .map(existing -> {
                    log.info("[작업지시 수신] 멱등 — 이미 등록됨 erpOrderNo={} workOrderNo={}",
                            existing.getErpOrderNo(), existing.getWorkOrderNo());
                    return new ReceiveOutcome(WorkOrderResponse.from(existing), false);
                })
                .orElseGet(() -> {
                    WorkOrder wo = WorkOrder.received(
                            toWorkOrderNo(req.erpOrderNo()), req.erpOrderNo(),
                            req.productCode(), req.productName(), req.quantity(),
                            req.plannedDate(), LocalDateTime.now());
                    if (req.components() != null) {
                        req.components().forEach(c ->
                                wo.addLine(c.componentCode(), c.componentName(), c.requiredQty(), c.unit()));
                    }
                    repository.save(wo);
                    log.info("[작업지시 수신] 신규 등록 erpOrderNo={} → workOrderNo={}",
                            wo.getErpOrderNo(), wo.getWorkOrderNo());
                    return new ReceiveOutcome(WorkOrderResponse.from(wo), true);
                });
    }

    @Transactional(readOnly = true)
    public List<WorkOrderResponse> list() {
        return repository.findAllWithLines().stream().map(WorkOrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public WorkOrderResponse findById(Long id) {
        return repository.findById(id)
                .map(WorkOrderResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("작업지시를 찾을 수 없습니다: " + id));
    }

    /** PO-20260604-001 → WO-20260604-001 (ERP 번호와 1:1 대응되게). */
    private static String toWorkOrderNo(String erpOrderNo) {
        return erpOrderNo.startsWith("PO-") ? "WO-" + erpOrderNo.substring(3) : "WO-" + erpOrderNo;
    }
}
