package com.hwlee.erp.sd.delivery;

import com.hwlee.erp.common.code.TransactionNumberGenerator;
import com.hwlee.erp.sd.delivery.dto.DeliveryCreateRequest;
import com.hwlee.erp.sd.delivery.dto.DeliveryLineRequest;
import com.hwlee.erp.sd.delivery.dto.DeliveryResponse;
import com.hwlee.erp.sd.order.SalesOrder;
import com.hwlee.erp.sd.order.SalesOrderLine;
import com.hwlee.erp.sd.order.SalesOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    private final DeliveryRepository repository;
    private final DeliveryMapper mapper;
    private final SalesOrderRepository salesOrderRepository;
    private final TransactionNumberGenerator numberGenerator;

    @Transactional
    public DeliveryResponse create(DeliveryCreateRequest req) {
        SalesOrder order = salesOrderRepository.findById(req.salesOrderId())
                .orElseThrow(() -> new EntityNotFoundException("SalesOrder not found: id=" + req.salesOrderId()));

        String number = numberGenerator.nextDeliveryNumber(req.shippedDate());
        Delivery delivery = Delivery.draft(number, order, req.shippedDate());

        for (DeliveryLineRequest lineReq : req.lines()) {
            SalesOrderLine sol = order.findLineById(lineReq.salesOrderLineId());
            delivery.addLine(sol, lineReq.quantity());
        }

        delivery.ship();
        // SO 라인의 shipped_qty 누적 + SO 헤더 상태 전이 (같은 트랜잭션)
        for (DeliveryLine dline : delivery.getLines()) {
            order.recordShipment(dline.getSalesOrderLine(), dline.getQuantity());
        }

        return mapper.toResponse(repository.save(delivery));
    }

    public DeliveryResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    public Page<DeliveryResponse> search(Specification<Delivery> spec, Pageable pageable) {
        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional
    public DeliveryResponse cancel(Long id) {
        Delivery delivery = getOrThrow(id);
        SalesOrder order = delivery.getSalesOrder();
        delivery.cancel();
        for (DeliveryLine dline : delivery.getLines()) {
            order.cancelShipment(dline.getSalesOrderLine(), dline.getQuantity());
        }
        return mapper.toResponse(delivery);
    }

    private Delivery getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Delivery not found: id=" + id));
    }
}
