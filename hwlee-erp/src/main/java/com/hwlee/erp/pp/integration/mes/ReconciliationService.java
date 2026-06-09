package com.hwlee.erp.pp.integration.mes;

import com.hwlee.erp.pp.order.ProductionOrder;
import com.hwlee.erp.pp.order.ProductionOrderRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ERP↔MES 정합성 검증 — ERP 가 MES 로 전송한 생산지시(dispatched)가 MES 작업지시와 일치하는지 대조.
 *
 * <p>분산 시스템은 네트워크/장애로 한쪽만 반영될 수 있다. 사후에 양쪽을 대조해 불일치를 찾아내는 것이
 * 운영의 핵심(정합성 검증 배치). 여기서는 누락(MISSING_IN_MES)과 수량 불일치(QTY_MISMATCH)를 본다.
 */
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final ProductionOrderRepository orderRepository;
    private final MesClient mesClient;

    @Transactional(readOnly = true)
    public ReconciliationResponse reconcile() {
        List<ProductionOrder> dispatched = orderRepository.findByMesWorkOrderNoIsNotNull();
        Map<String, MesWorkOrderSummary> mesByErpNo = mesClient.fetchWorkOrders().stream()
                .collect(Collectors.toMap(MesWorkOrderSummary::erpOrderNo, Function.identity(), (a, b) -> a));

        List<ReconciliationResponse.Discrepancy> issues = new ArrayList<>();
        for (ProductionOrder po : dispatched) {
            MesWorkOrderSummary wo = mesByErpNo.get(po.getNumber());
            if (wo == null) {
                issues.add(new ReconciliationResponse.Discrepancy(
                        po.getNumber(), "MISSING_IN_MES", "ERP 가 전송했으나 MES 에 작업지시가 없음"));
            } else if (po.getQuantity().compareTo(wo.quantity()) != 0) {
                issues.add(new ReconciliationResponse.Discrepancy(
                        po.getNumber(), "QTY_MISMATCH",
                        "ERP=" + po.getQuantity() + " / MES=" + wo.quantity()));
            }
        }
        return new ReconciliationResponse(LocalDateTime.now(), dispatched.size(), issues.isEmpty(), issues);
    }
}
