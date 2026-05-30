package com.hwlee.erp.master.department;

import com.hwlee.erp.master.department.dto.DepartmentCreateRequest;
import com.hwlee.erp.master.department.dto.DepartmentResponse;
import com.hwlee.erp.master.department.dto.DepartmentUpdateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SALES','PURCHASING','FINANCE','ADMIN')")
public class DepartmentController {

    private final DepartmentService service;

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody DepartmentCreateRequest req) {
        DepartmentResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/departments/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public DepartmentResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/by-code/{code}")
    public DepartmentResponse findByCode(@PathVariable String code) {
        return service.findByCode(code);
    }

    @GetMapping
    public List<DepartmentResponse> findAll() {
        // 부서는 보통 수십 개 정도라 페이징 없이 전체 반환.
        return service.findAll();
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public DepartmentResponse update(@PathVariable Long id, @Valid @RequestBody DepartmentUpdateRequest req) {
        return service.update(id, req);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
