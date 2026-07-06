package com.hwlee.erp.master.vendoritem;

import com.hwlee.erp.common.entity.MasterStatus;
import com.hwlee.erp.master.vendoritem.dto.VendorItemCreateRequest;
import com.hwlee.erp.master.vendoritem.dto.VendorItemResponse;
import com.hwlee.erp.master.vendoritem.dto.VendorItemUpdateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 거래처 취급품목(구매정보레코드) API — 조회는 구매/영업/재무, 변경은 구매·관리자.
 * 입고 화면이 {@code GET /api/vendor-items?vendorId=&status=ACTIVE} 로 거래처별 취급품목을 부른다.
 */
@RestController
@RequestMapping("/api/vendor-items")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES','PURCHASING','FINANCE','PRODUCTION','ADMIN')")
public class VendorItemController {

    private final VendorItemService service;

    @PreAuthorize("hasAnyRole('PURCHASING','ADMIN')")
    @PostMapping
    public ResponseEntity<VendorItemResponse> create(@Valid @RequestBody VendorItemCreateRequest req) {
        VendorItemResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/vendor-items/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public VendorItemResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    public Page<VendorItemResponse> search(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) MasterStatus status,
            Pageable pageable
    ) {
        Specification<VendorItem> spec = Specification.allOf(
                vendorIdEquals(vendorId), itemIdEquals(itemId), statusEquals(status));
        return service.search(spec, pageable);
    }

    @PreAuthorize("hasAnyRole('PURCHASING','ADMIN')")
    @PutMapping("/{id}")
    public VendorItemResponse update(@PathVariable Long id, @Valid @RequestBody VendorItemUpdateRequest req) {
        return service.update(id, req);
    }

    @PreAuthorize("hasAnyRole('PURCHASING','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static Specification<VendorItem> vendorIdEquals(Long vendorId) {
        if (vendorId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("vendor").get("id"), vendorId);
    }

    private static Specification<VendorItem> itemIdEquals(Long itemId) {
        if (itemId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("item").get("id"), itemId);
    }

    private static Specification<VendorItem> statusEquals(MasterStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
