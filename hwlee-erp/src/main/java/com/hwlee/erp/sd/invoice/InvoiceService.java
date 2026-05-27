package com.hwlee.erp.sd.invoice;

import com.hwlee.erp.common.code.TransactionNumberGenerator;
import com.hwlee.erp.sd.invoice.dto.InvoiceCreateRequest;
import com.hwlee.erp.sd.invoice.dto.InvoiceLineRequest;
import com.hwlee.erp.sd.invoice.dto.InvoiceResponse;
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
public class InvoiceService {

    private final InvoiceRepository repository;
    private final InvoiceMapper mapper;
    private final SalesOrderRepository salesOrderRepository;
    private final TransactionNumberGenerator numberGenerator;

    @Transactional
    public InvoiceResponse create(InvoiceCreateRequest req) {
        SalesOrder order = salesOrderRepository.findById(req.salesOrderId())
                .orElseThrow(() -> new EntityNotFoundException("SalesOrder not found: id=" + req.salesOrderId()));

        String number = numberGenerator.nextInvoiceNumber(req.invoiceDate());
        Invoice invoice = Invoice.draft(number, order, req.invoiceDate());

        for (InvoiceLineRequest lineReq : req.lines()) {
            SalesOrderLine sol = order.findLineById(lineReq.salesOrderLineId());
            invoice.addLine(sol, lineReq.quantity());
        }

        // 발행과 SO 라인 누적은 같은 트랜잭션 안에서 묶임
        invoice.issue();
        for (InvoiceLine iline : invoice.getLines()) {
            order.recordInvoicing(iline.getSalesOrderLine(), iline.getQuantity());
        }

        return mapper.toResponse(repository.save(invoice));
    }

    public InvoiceResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    public Page<InvoiceResponse> search(Specification<Invoice> spec, Pageable pageable) {
        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional
    public InvoiceResponse cancel(Long id) {
        Invoice invoice = getOrThrow(id);
        SalesOrder order = invoice.getSalesOrder();
        invoice.cancel();
        for (InvoiceLine iline : invoice.getLines()) {
            order.cancelInvoicing(iline.getSalesOrderLine(), iline.getQuantity());
        }
        return mapper.toResponse(invoice);
    }

    private Invoice getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found: id=" + id));
    }
}
