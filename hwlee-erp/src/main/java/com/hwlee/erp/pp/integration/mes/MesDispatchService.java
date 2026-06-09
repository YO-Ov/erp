package com.hwlee.erp.pp.integration.mes;

import com.hwlee.erp.pp.order.ProductionOrder;
import com.hwlee.erp.pp.order.ProductionOrderRepository;
import com.hwlee.erp.pp.order.ProductionOrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 생산지시(PO)를 MES 작업지시로 전송하는 오케스트레이션.
 *
 * <p>RELEASED(착수)된 생산지시만 현장(MES)에 내린다. {@link MesClient} 가 회복성(재시도·서킷브레이커)을
 * 담당하고, 멱등성은 MES 가 {@code erpOrderNo} 로 보장한다 — 여러 번 호출해도 MES 엔 1건만 등록된다.
 */
@Service
@RequiredArgsConstructor
public class MesDispatchService {

    private final ProductionOrderRepository orderRepository;
    private final MesClient mesClient;

    @Transactional
    public DispatchResult dispatch(Long productionOrderId) {
        ProductionOrder po = orderRepository.findByIdWithLines(productionOrderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "생산지시를 찾을 수 없습니다: " + productionOrderId));

        if (po.getStatus() != ProductionOrderStatus.RELEASED
                && po.getStatus() != ProductionOrderStatus.COMPLETED) {
            throw new IllegalStateException(
                    "착수(RELEASED)된 생산지시만 MES로 전송할 수 있습니다. 현재: " + po.getStatus());
        }

        MesWorkOrderResponse res = mesClient.sendWorkOrder(toRequest(po));
        po.markDispatched(res.workOrderNo(), LocalDateTime.now());

        return new DispatchResult(po.getNumber(), res.workOrderNo(), res.status(), po.getMesDispatchedAt());
    }

    private static MesWorkOrderRequest toRequest(ProductionOrder po) {
        List<MesWorkOrderRequest.ComponentLine> components = po.getLines().stream()
                .map(l -> new MesWorkOrderRequest.ComponentLine(
                        l.getComponent().getCode(),
                        l.getComponent().getName(),
                        l.getRequiredQty(),
                        l.getComponent().getUnit() != null ? l.getComponent().getUnit().name() : null))
                .toList();
        return new MesWorkOrderRequest(
                po.getNumber(),
                po.getProduct().getCode(),
                po.getProduct().getName(),
                po.getQuantity(),
                po.getOrderDate(),
                components);
    }
}
