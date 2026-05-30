package com.hwlee.erp.master.vendor;

import com.hwlee.erp.common.entity.MasterStatus;
import com.hwlee.erp.master.vendor.dto.VendorCreateRequest;
import com.hwlee.erp.master.vendor.dto.VendorResponse;
import com.hwlee.erp.master.vendor.dto.VendorUpdateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SALES','PURCHASING','FINANCE','ADMIN')")
public class VendorController {

    private final VendorService service;

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<VendorResponse> create(@Valid @RequestBody VendorCreateRequest req) {
        VendorResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/vendors/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public VendorResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/by-code/{code}")
    public VendorResponse findByCode(@PathVariable String code) {
        return service.findByCode(code);
    }

    @GetMapping
    public Page<VendorResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String businessNo,
            @RequestParam(required = false) MasterStatus status,
            Pageable pageable
    ) {
        Specification<Vendor> spec = Specification.allOf(
                nameContains(name),
                businessNoEquals(businessNo),
                statusEquals(status)
        );
        return service.search(spec, pageable);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public VendorResponse update(@PathVariable Long id, @Valid @RequestBody VendorUpdateRequest req) {
        return service.update(id, req);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static Specification<Vendor> nameContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return (root, query, cb) -> cb.like(root.get("name"), "%" + keyword + "%");
    }

    private static Specification<Vendor> businessNoEquals(String businessNo) {
        if (businessNo == null || businessNo.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("businessNo"), businessNo);
    }

    private static Specification<Vendor> statusEquals(MasterStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
