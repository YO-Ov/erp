package com.hwlee.erp.sd.order;

import com.hwlee.erp.master.customer.Customer;
import com.hwlee.erp.master.customer.CustomerRepository;
import com.hwlee.erp.master.employee.Employee;
import com.hwlee.erp.master.employee.EmployeeRepository;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemRepository;
import com.hwlee.erp.common.code.TransactionNumberGenerator;
import com.hwlee.erp.sd.order.creditcheck.CreditLimitChecker;
import com.hwlee.erp.sd.order.event.SalesOrderConfirmedEvent;
import com.hwlee.erp.sd.order.dto.CreditStatusResponse;
import com.hwlee.erp.sd.order.dto.SalesOrderCreateRequest;
import com.hwlee.erp.sd.order.dto.SalesOrderLineRequest;
import com.hwlee.erp.sd.order.dto.SalesOrderResponse;
import com.hwlee.erp.sd.order.dto.SalesOrderUpdateRequest;
import com.hwlee.erp.sd.quotation.Quotation;
import com.hwlee.erp.sd.quotation.QuotationRepository;
import com.hwlee.erp.sd.quotation.QuotationStatus;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesOrderService {

    private final SalesOrderRepository repository;
    private final SalesOrderMapper mapper;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final ItemRepository itemRepository;
    private final QuotationRepository quotationRepository;
    private final TransactionNumberGenerator numberGenerator;
    private final CreditLimitChecker creditLimitChecker;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public SalesOrderResponse create(SalesOrderCreateRequest req) {
        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: id=" + req.customerId()));
        Employee salesperson = resolveSalesperson(req.salespersonId());
        Quotation quotation = resolveQuotation(req.quotationId());
        if (quotation != null) {
            // 견적당 수주 1건 — ACCEPTED → CONVERTED. 이미 전환된 견적이면 markConverted 가 거부한다.
            quotation.markConverted();
        }
        String number = numberGenerator.nextSalesOrderNumber(req.orderDate());
        SalesOrder order = SalesOrder.draft(number, customer, salesperson, quotation, req.orderDate());
        addLines(order, req.lines());
        return mapper.toResponse(repository.save(order));
    }

    public SalesOrderResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    /**
     * 고객 신용한도 현황 — 수주 화면이 확정 전에 "남은 한도" 를 미리 보여주기 위해 호출한다.
     * 검증({@link CreditLimitChecker})과 같은 산식(creditLimit - 활성 수주 합계)을 조회용으로 노출한다.
     */
    public CreditStatusResponse creditStatus(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: id=" + customerId));
        BigDecimal used = repository.sumActiveOrderAmountByCustomer(customerId, null);
        if (used == null) used = BigDecimal.ZERO;
        BigDecimal remaining = customer.getCreditLimit().subtract(used);
        return new CreditStatusResponse(customerId, customer.getCreditLimit(), used, remaining);
    }

    public Page<SalesOrderResponse> search(Specification<SalesOrder> spec, Pageable pageable) {
        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional
    public SalesOrderResponse update(Long id, SalesOrderUpdateRequest req) {
        SalesOrder order = getOrThrow(id);
        Employee salesperson = resolveSalesperson(req.salespersonId());
        order.updateHeader(salesperson, req.orderDate());
        order.clearLines();
        addLines(order, req.lines());
        return mapper.toResponse(order);
    }

    @Transactional
    public SalesOrderResponse confirm(Long id) {
        SalesOrder order = getOrThrow(id);
        creditLimitChecker.check(order.getCustomer(), order.getTotalAmount(), order.getId());
        order.confirm(LocalDateTime.now(clock));
        // ⭐ 수주 확정 사건 발행 — PP 리스너가 BEFORE_COMMIT 에서 완제품 부족분만큼 계획오더(MRP) 자동 생성.
        events.publishEvent(new SalesOrderConfirmedEvent(
                order.getId(), order.getNumber(),
                order.getLines().stream()
                        .map(l -> new SalesOrderConfirmedEvent.Line(l.getItem().getId(), l.getOrderQty()))
                        .toList()));
        return mapper.toResponse(order);
    }

    @Transactional
    public SalesOrderResponse cancel(Long id) {
        SalesOrder order = getOrThrow(id);
        order.cancel();
        // 견적으로 만든 수주가 취소되면 그 견적을 ACCEPTED 로 되살려 재사용 가능하게 한다.
        Quotation quotation = order.getQuotation();
        if (quotation != null && quotation.getStatus() == QuotationStatus.CONVERTED) {
            quotation.revertConversion();
        }
        return mapper.toResponse(order);
    }

    /**
     * 전량 청구(INVOICED)된 수주를 CLOSED(거래 종료)로 마감한다. 수금 여부와 무관 — FI Payment 소관.
     */
    @Transactional
    public SalesOrderResponse close(Long id) {
        SalesOrder order = getOrThrow(id);
        order.close();
        return mapper.toResponse(order);
    }

    private void addLines(SalesOrder order, List<SalesOrderLineRequest> lineReqs) {
        for (SalesOrderLineRequest lineReq : lineReqs) {
            Item item = itemRepository.findById(lineReq.itemId())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found: id=" + lineReq.itemId()));
            order.addLine(item, lineReq.orderQty(), lineReq.unitPrice());
        }
    }

    private Employee resolveSalesperson(Long id) {
        if (id == null) return null;
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found: id=" + id));
    }

    private Quotation resolveQuotation(Long id) {
        if (id == null) return null;
        return quotationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Quotation not found: id=" + id));
    }

    SalesOrder getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SalesOrder not found: id=" + id));
    }
}
