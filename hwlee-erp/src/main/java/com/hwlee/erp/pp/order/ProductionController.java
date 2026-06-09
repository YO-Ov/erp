package com.hwlee.erp.pp.order;

import com.hwlee.erp.pp.integration.mes.DispatchResult;
import com.hwlee.erp.pp.integration.mes.MesDispatchService;
import com.hwlee.erp.pp.order.dto.MaterialAvailabilityResponse;
import com.hwlee.erp.pp.order.dto.ProductionOrderCreateRequest;
import com.hwlee.erp.pp.order.dto.ProductionOrderResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 생산지시 API — 생산(PP) 권한. */
@RestController
@RequestMapping("/api/production-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCTION','ADMIN')")
public class ProductionController {

    private final ProductionService service;
    private final MesDispatchService mesDispatchService;

    @PostMapping
    public ResponseEntity<ProductionOrderResponse> create(@Valid @RequestBody ProductionOrderCreateRequest req) {
        ProductionOrderResponse created = service.createDraft(req);
        return ResponseEntity.created(URI.create("/api/production-orders/" + created.id())).body(created);
    }

    @GetMapping
    public Page<ProductionOrderResponse> search(
            @RequestParam(required = false) ProductionOrderStatus status,
            @RequestParam(required = false) Long productItemId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable) {
        return service.search(status, productItemId, dateFrom, dateTo, pageable);
    }

    @GetMapping("/{id}")
    public ProductionOrderResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/material-availability")
    public MaterialAvailabilityResponse materialAvailability(@PathVariable Long id) {
        return service.materialAvailability(id);
    }

    @PostMapping("/{id}/release")
    public ProductionOrderResponse release(@PathVariable Long id) {
        return service.release(id);
    }

    @PostMapping("/{id}/complete")
    public ProductionOrderResponse complete(@PathVariable Long id) {
        return service.complete(id);
    }

    @PostMapping("/{id}/cancel")
    public ProductionOrderResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    /** Phase 12 — 착수된 생산지시를 MES로 작업지시 전송(동기 REST, 멱등). */
    @PostMapping("/{id}/dispatch")
    public DispatchResult dispatch(@PathVariable Long id) {
        return mesDispatchService.dispatch(id);
    }
}
