package com.hwlee.erp.sd.order;

import static com.hwlee.erp.sd.order.SalesOrderSpecifications.customerIdEquals;
import static com.hwlee.erp.sd.order.SalesOrderSpecifications.orderedFrom;
import static com.hwlee.erp.sd.order.SalesOrderSpecifications.orderedTo;
import static com.hwlee.erp.sd.order.SalesOrderSpecifications.salespersonIdEquals;
import static com.hwlee.erp.sd.order.SalesOrderSpecifications.statusEquals;
import static org.springframework.data.jpa.domain.Specification.where;

import com.hwlee.erp.sd.order.dto.SalesOrderCreateRequest;
import com.hwlee.erp.sd.order.dto.SalesOrderResponse;
import com.hwlee.erp.sd.order.dto.SalesOrderUpdateRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales-orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService service;

    @PostMapping
    public ResponseEntity<SalesOrderResponse> create(@Valid @RequestBody SalesOrderCreateRequest req) {
        SalesOrderResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/sales-orders/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public SalesOrderResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    public Page<SalesOrderResponse> search(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long salespersonId,
            @RequestParam(required = false) SalesOrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable
    ) {
        return service.search(
                where(customerIdEquals(customerId))
                        .and(salespersonIdEquals(salespersonId))
                        .and(statusEquals(status))
                        .and(orderedFrom(dateFrom))
                        .and(orderedTo(dateTo)),
                pageable
        );
    }

    @PutMapping("/{id}")
    public SalesOrderResponse update(@PathVariable Long id, @Valid @RequestBody SalesOrderUpdateRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/{id}/confirm")
    public SalesOrderResponse confirm(@PathVariable Long id) {
        return service.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    public SalesOrderResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
