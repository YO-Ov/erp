package com.hwlee.erp.pp.bom;

import com.hwlee.erp.pp.bom.dto.BomCreateRequest;
import com.hwlee.erp.pp.bom.dto.BomResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** BOM API — 생산(PP) 권한. */
@RestController
@RequestMapping("/api/boms")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCTION','ADMIN')")
public class BomController {

    private final BomService service;

    @PostMapping
    public ResponseEntity<BomResponse> create(@Valid @RequestBody BomCreateRequest req) {
        BomResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/boms/" + created.id())).body(created);
    }

    @GetMapping
    public List<BomResponse> findByProduct(@RequestParam Long productItemId) {
        return service.findByProduct(productItemId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
