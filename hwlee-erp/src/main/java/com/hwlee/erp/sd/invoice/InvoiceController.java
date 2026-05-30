package com.hwlee.erp.sd.invoice;

import static com.hwlee.erp.sd.invoice.InvoiceSpecifications.issuedFrom;
import static com.hwlee.erp.sd.invoice.InvoiceSpecifications.issuedTo;
import static com.hwlee.erp.sd.invoice.InvoiceSpecifications.salesOrderIdEquals;
import static com.hwlee.erp.sd.invoice.InvoiceSpecifications.statusEquals;
import static org.springframework.data.jpa.domain.Specification.where;

import com.hwlee.erp.sd.invoice.dto.InvoiceCreateRequest;
import com.hwlee.erp.sd.invoice.dto.InvoiceResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SALES','ADMIN')")
public class InvoiceController {

    private final InvoiceService service;

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceCreateRequest req) {
        InvoiceResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/invoices/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public InvoiceResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    public Page<InvoiceResponse> search(
            @RequestParam(required = false) Long salesOrderId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable
    ) {
        return service.search(
                where(salesOrderIdEquals(salesOrderId))
                        .and(statusEquals(status))
                        .and(issuedFrom(dateFrom))
                        .and(issuedTo(dateTo)),
                pageable
        );
    }

    @PostMapping("/{id}/cancel")
    public InvoiceResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
