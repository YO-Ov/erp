package com.hwlee.erp.mm.purchaseorder;

import static com.hwlee.erp.mm.purchaseorder.PurchaseOrderSpecifications.orderFrom;
import static com.hwlee.erp.mm.purchaseorder.PurchaseOrderSpecifications.orderTo;
import static com.hwlee.erp.mm.purchaseorder.PurchaseOrderSpecifications.statusEquals;
import static com.hwlee.erp.mm.purchaseorder.PurchaseOrderSpecifications.vendorIdEquals;
import static com.hwlee.erp.mm.purchaseorder.PurchaseOrderSpecifications.warehouseIdEquals;
import static org.springframework.data.jpa.domain.Specification.where;

import com.hwlee.erp.approval.dto.ApprovalResponse;
import com.hwlee.erp.mm.purchaseorder.dto.PurchaseOrderCreateRequest;
import com.hwlee.erp.mm.purchaseorder.dto.PurchaseOrderResponse;
import com.hwlee.erp.mm.purchaseorder.dto.PurchaseOrderUpdateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
// 발주 조회(GET)는 결재선에 낄 수 있는 여러 부서(팀장·본부장 DIRECTOR·재무 합의자)가 결재함에서
// 원본을 열람해야 하므로 업무 역할에 넓게 허용한다. 쓰기·상신·종료·취소는 구매/관리자로 메서드 단위에서 좁힌다.
@PreAuthorize("hasAnyRole('SALES','PURCHASING','PRODUCTION','FINANCE','DIRECTOR','ADMIN')")
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('PURCHASING','ADMIN')")
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody PurchaseOrderCreateRequest req) {
        PurchaseOrderResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/purchase-orders/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public PurchaseOrderResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    public Page<PurchaseOrderResponse> search(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable
    ) {
        return service.search(
                where(vendorIdEquals(vendorId))
                        .and(warehouseIdEquals(warehouseId))
                        .and(statusEquals(status))
                        .and(orderFrom(dateFrom))
                        .and(orderTo(dateTo)),
                pageable
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PURCHASING','ADMIN')")
    public PurchaseOrderResponse update(@PathVariable Long id,
                                        @Valid @RequestBody PurchaseOrderUpdateRequest req) {
        return service.update(id, req);
    }

    /** 결재 상신 — 작성 중 발주를 전자결재에 올린다. 최종 승인 시 발주 확정(CONFIRMED). */
    @PostMapping("/{id}/submit-approval")
    @PreAuthorize("hasAnyRole('PURCHASING','ADMIN')")
    public ApprovalResponse submitForApproval(@PathVariable Long id, Principal principal) {
        return service.submitForApproval(id, principal.getName());
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('PURCHASING','ADMIN')")
    public PurchaseOrderResponse close(@PathVariable Long id) {
        return service.close(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PURCHASING','ADMIN')")
    public PurchaseOrderResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
