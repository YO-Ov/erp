package com.hwlee.erp.sd.order;

import com.hwlee.erp.master.customer.Customer;
import com.hwlee.erp.master.customer.CustomerRepository;
import com.hwlee.erp.master.employee.Employee;
import com.hwlee.erp.master.employee.EmployeeRepository;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemRepository;
import com.hwlee.erp.sd.TransactionNumberGenerator;
import com.hwlee.erp.sd.order.creditcheck.CreditLimitChecker;
import com.hwlee.erp.sd.order.dto.SalesOrderCreateRequest;
import com.hwlee.erp.sd.order.dto.SalesOrderLineRequest;
import com.hwlee.erp.sd.order.dto.SalesOrderResponse;
import com.hwlee.erp.sd.order.dto.SalesOrderUpdateRequest;
import com.hwlee.erp.sd.quotation.Quotation;
import com.hwlee.erp.sd.quotation.QuotationRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
    private final Clock clock;

    @Transactional
    public SalesOrderResponse create(SalesOrderCreateRequest req) {
        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: id=" + req.customerId()));
        Employee salesperson = resolveSalesperson(req.salespersonId());
        Quotation quotation = resolveQuotation(req.quotationId());
        String number = numberGenerator.nextSalesOrderNumber(req.orderDate());
        SalesOrder order = SalesOrder.draft(number, customer, salesperson, quotation, req.orderDate());
        addLines(order, req.lines());
        return mapper.toResponse(repository.save(order));
    }

    public SalesOrderResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
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
        return mapper.toResponse(order);
    }

    @Transactional
    public SalesOrderResponse cancel(Long id) {
        SalesOrder order = getOrThrow(id);
        order.cancel();
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
