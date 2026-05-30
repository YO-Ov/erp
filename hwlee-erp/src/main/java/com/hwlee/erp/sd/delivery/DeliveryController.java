package com.hwlee.erp.sd.delivery;

import static com.hwlee.erp.sd.delivery.DeliverySpecifications.salesOrderIdEquals;
import static com.hwlee.erp.sd.delivery.DeliverySpecifications.shippedFrom;
import static com.hwlee.erp.sd.delivery.DeliverySpecifications.shippedTo;
import static com.hwlee.erp.sd.delivery.DeliverySpecifications.statusEquals;
import static org.springframework.data.jpa.domain.Specification.where;

import com.hwlee.erp.sd.delivery.dto.DeliveryCreateRequest;
import com.hwlee.erp.sd.delivery.dto.DeliveryResponse;
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
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SALES','ADMIN')")
public class DeliveryController {

    private final DeliveryService service;

    @PostMapping
    public ResponseEntity<DeliveryResponse> create(@Valid @RequestBody DeliveryCreateRequest req) {
        DeliveryResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/deliveries/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public DeliveryResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    public Page<DeliveryResponse> search(
            @RequestParam(required = false) Long salesOrderId,
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable
    ) {
        return service.search(
                where(salesOrderIdEquals(salesOrderId))
                        .and(statusEquals(status))
                        .and(shippedFrom(dateFrom))
                        .and(shippedTo(dateTo)),
                pageable
        );
    }

    @PostMapping("/{id}/cancel")
    public DeliveryResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
