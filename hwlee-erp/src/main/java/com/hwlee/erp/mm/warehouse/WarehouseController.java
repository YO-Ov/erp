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
// 창고 조회(GET)는 출하·재고 등 여러 업무 화면이 참조하는 마스터라 업무 역할에 넓게 허용한다.
// 쓰기(생성/수정/삭제)는 창고 마스터를 관리하는 구매·생산·관리자로 메서드 단위에서 좁힌다.
@org.springframework.security.access.prepost.PreAuthorize(
        "hasAnyRole('SALES','PURCHASING','PRODUCTION','FINANCE','ADMIN')")
public class WarehouseController {

    private final WarehouseService service;

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('PURCHASING','PRODUCTION','ADMIN')")
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
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('PURCHASING','PRODUCTION','ADMIN')")
    public WarehouseResponse update(@PathVariable Long id,
                                    @Valid @RequestBody WarehouseUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('PURCHASING','PRODUCTION','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
