package com.hwlee.erp.pp.planning;

import com.hwlee.erp.common.code.TransactionNumberGenerator;
import com.hwlee.erp.master.employee.Employee;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemRepository;
import com.hwlee.erp.master.item.ItemType;
import com.hwlee.erp.mm.stock.StockRepository;
import com.hwlee.erp.notification.NotificationService;
import com.hwlee.erp.notification.NotificationType;
import com.hwlee.erp.pp.order.ProductionService;
import com.hwlee.erp.sd.order.SalesOrder;
import com.hwlee.erp.sd.order.SalesOrderRepository;
import com.hwlee.erp.pp.order.dto.ProductionOrderCreateRequest;
import com.hwlee.erp.pp.order.dto.ProductionOrderResponse;
import com.hwlee.erp.pp.planning.dto.PlannedOrderConvertRequest;
import com.hwlee.erp.pp.planning.dto.PlannedOrderResponse;
import com.hwlee.erp.sd.order.event.SalesOrderConfirmedEvent;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계획오더(MRP 제안) 서비스 — 수주 확정 사건을 받아 부족분만큼 제안을 생성하고(자동),
 * 담당자의 승인(생산지시 전환)·기각을 처리한다.
 *
 * <p>핵심 흐름: {@link #createFromSalesOrder}(수주 확정 리스너가 호출) → 완제품별 주문량 vs 현재고
 * 비교 → 부족분 있으면 PROPOSED 계획오더 저장. {@link #convert} 가 기존
 * {@link ProductionService#createDraft} 를 재사용해 생산지시(PLANNED)를 만든다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlannedOrderService {

    private final PlannedOrderRepository repository;
    private final ItemRepository itemRepository;
    private final StockRepository stockRepository;
    private final ProductionService productionService;
    private final SalesOrderRepository salesOrderRepository;
    private final NotificationService notificationService;
    private final TransactionNumberGenerator numberGenerator;
    private final Clock clock;

    /**
     * 수주 확정 사건 처리 — 완제품 라인마다 (주문량 − 전 창고 보유량) > 0 이면 계획오더를 제안한다.
     * 재고가 충분하면 아무것도 만들지 않고 조용히 끝난다(대부분의 경우). 수주 확정 트랜잭션에
     * 합류하므로, 여기서 예외가 나면 수주 확정 자체가 롤백된다.
     */
    @Transactional
    public void createFromSalesOrder(SalesOrderConfirmedEvent event) {
        LocalDate today = LocalDate.now(clock);
        for (SalesOrderConfirmedEvent.Line line : event.lines()) {
            Item product = itemRepository.findById(line.itemId())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found: id=" + line.itemId()));
            // 완제품만 생산 대상 — 부품/원재료를 파는 주문(드묾)은 MRP 제안 대상이 아니다.
            if (product.getItemType() != ItemType.FINISHED) continue;

            BigDecimal onHand = stockRepository.sumOnHandByItem(product.getId());
            if (onHand == null) onHand = BigDecimal.ZERO;
            BigDecimal shortage = line.orderQty().subtract(onHand);
            if (shortage.signum() <= 0) continue; // 재고 충분 → 제안 없음

            String number = numberGenerator.nextPlannedOrderNumber(today);
            PlannedOrder planned = PlannedOrder.propose(
                    number, product, line.orderQty(), onHand,
                    event.salesOrderId(), event.salesOrderNumber(), today);
            repository.save(planned);
            log.info("계획오더 제안: {} (완제품 {} 부족분 {} = 주문 {} - 현재고 {}, 수주 {})",
                    number, product.getCode(), shortage, line.orderQty(), onHand, event.salesOrderNumber());
        }
    }

    /**
     * 승인 → 생산지시 전환. 담당자가 창고(+납기)를 지정하면 부족분 수량으로 생산지시(PLANNED)를 만든다.
     * 생산지시 생성은 기존 {@link ProductionService#createDraft} 재사용 — BOM 미등록 완제품이면 거기서
     * 예외가 나고 전환은 롤백된다(제안은 가능했어도 실제 생산 불가).
     */
    @Transactional
    public ProductionOrderResponse convert(Long id, PlannedOrderConvertRequest req) {
        PlannedOrder planned = getOrThrow(id);
        ProductionOrderResponse production = productionService.createDraft(new ProductionOrderCreateRequest(
                planned.getProduct().getId(), req.warehouseId(),
                planned.getShortageQty(), LocalDate.now(clock), req.dueDate()));
        planned.markConverted(production.number());
        log.info("계획오더 전환: {} → 생산지시 {}", planned.getNumber(), production.number());
        return production;
    }

    /** 기각 — 이번엔 생산하지 않음. */
    @Transactional
    public PlannedOrderResponse dismiss(Long id) {
        PlannedOrder planned = getOrThrow(id);
        planned.dismiss();
        return toResponse(planned);
    }

    /**
     * 생산지시 취소에 따른 되살리기 — 해당 번호로 전환됐던 계획오더를 검토 대기(PROPOSED)로 복원한다.
     * 계획오더에서 안 온 생산지시(수동 생성)거나 이미 PROPOSED 면 조용히 넘어간다.
     */
    @Transactional
    public void revertByProductionNumber(String productionNumber) {
        repository.findByConvertedProductionNumber(productionNumber).ifPresent(p -> {
            if (p.getStatus() == PlannedOrderStatus.CONVERTED) {
                p.revertConversion();
                log.info("생산지시 {} 취소 → 계획오더 {} 검토 대기(PROPOSED)로 복원 (수주 {} 부족분 미해결)",
                        productionNumber, p.getNumber(), p.getSourceSalesOrderNumber());
                notifySalesOfCancellation(p, productionNumber);
            }
        });
    }

    /**
     * 생산취소 → 영업 알림. 출처 수주의 영업담당(salesperson)이 지정돼 있으면 그 사람에게,
     * 없으면 SALES 역할 전체에게. 클릭하면 해당 수주 화면으로 이동(딥링크).
     */
    private void notifySalesOfCancellation(PlannedOrder p, String productionNumber) {
        String title = "생산 취소 — 재생산 검토 필요";
        String message = String.format(
                "수주 %s 의 생산(%s)이 취소되어 부족분 %s개가 다시 미해결입니다. 계획오더(MRP)에서 재생산을 검토하세요.",
                p.getSourceSalesOrderNumber(), productionNumber,
                p.getShortageQty().stripTrailingZeros().toPlainString());
        String link = p.getSourceSalesOrderId() != null
                ? "/sd/sales-orders/" + p.getSourceSalesOrderId()
                : "/pp/planned-orders";

        String salesUsername = null;
        if (p.getSourceSalesOrderId() != null) {
            salesUsername = salesOrderRepository.findById(p.getSourceSalesOrderId())
                    .map(SalesOrder::getSalesperson)
                    .map(Employee::getEmail)
                    .orElse(null);
        }
        if (salesUsername != null)
            notificationService.notifyUser(salesUsername, NotificationType.PRODUCTION_CANCELLED, title, message, link);
        else
            notificationService.notifyRole("SALES", NotificationType.PRODUCTION_CANCELLED, title, message, link);
    }

    public PlannedOrderResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    public Page<PlannedOrderResponse> search(PlannedOrderStatus status, Pageable pageable) {
        Specification<PlannedOrder> spec = (root, query, cb) -> cb.conjunction();
        if (status != null)
            spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        return repository.findAll(spec, pageable).map(this::toResponse);
    }

    private PlannedOrder getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PlannedOrder not found: id=" + id));
    }

    private PlannedOrderResponse toResponse(PlannedOrder p) {
        return new PlannedOrderResponse(
                p.getId(), p.getNumber(),
                p.getProduct().getId(), p.getProduct().getCode(), p.getProduct().getName(),
                p.getRequiredQty(), p.getOnHandQty(), p.getShortageQty(), p.getStatus(),
                p.getSourceSalesOrderId(), p.getSourceSalesOrderNumber(), p.getConvertedProductionNumber(),
                p.getOrderDate(), p.getCreatedAt(), p.getCreatedBy());
    }
}
