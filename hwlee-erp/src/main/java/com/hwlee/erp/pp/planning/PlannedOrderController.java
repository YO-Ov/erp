package com.hwlee.erp.pp.planning;

import com.hwlee.erp.pp.order.dto.ProductionOrderResponse;
import com.hwlee.erp.pp.planning.dto.PlannedOrderConvertRequest;
import com.hwlee.erp.pp.planning.dto.PlannedOrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/planned-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCTION','ADMIN')")
public class PlannedOrderController {

    private final PlannedOrderService service;

    @GetMapping
    public Page<PlannedOrderResponse> search(
            @RequestParam(required = false) PlannedOrderStatus status,
            Pageable pageable) {
        return service.search(status, pageable);
    }

    @GetMapping("/{id}")
    public PlannedOrderResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    /** 승인 → 생산지시 전환 (창고·납기 지정). */
    @PostMapping("/{id}/convert")
    public ProductionOrderResponse convert(@PathVariable Long id,
                                           @Valid @RequestBody PlannedOrderConvertRequest req) {
        return service.convert(id, req);
    }

    /** 기각 — 이번엔 생산하지 않음. */
    @PostMapping("/{id}/dismiss")
    public PlannedOrderResponse dismiss(@PathVariable Long id) {
        return service.dismiss(id);
    }
}
