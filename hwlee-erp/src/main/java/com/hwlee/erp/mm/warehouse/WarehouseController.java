package com.hwlee.erp.mm.warehouse;

import static com.hwlee.erp.mm.warehouse.WarehouseSpecifications.nameContains;
import static com.hwlee.erp.mm.warehouse.WarehouseSpecifications.statusEquals;
import static org.springframework.data.jpa.domain.Specification.where;

import com.hwlee.erp.common.entity.MasterStatus;
import com.hwlee.erp.mm.warehouse.dto.WarehouseCreateRequest;
import com.hwlee.erp.mm.warehouse.dto.WarehouseResponse;
import com.hwlee.erp.mm.warehouse.dto.WarehouseUpdateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService service;

    @PostMapping
    public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody WarehouseCreateRequest req) {
        WarehouseResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/warehouses/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public WarehouseResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/by-code/{code}")
    public WarehouseResponse findByCode(@PathVariable String code) {
        return service.findByCode(code);
    }

    @GetMapping
    public Page<WarehouseResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) MasterStatus status,
            Pageable pageable
    ) {
        return service.search(where(nameContains(name)).and(statusEquals(status)), pageable);
    }

    @PutMapping("/{id}")
    public WarehouseResponse update(@PathVariable Long id,
                                    @Valid @RequestBody WarehouseUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
