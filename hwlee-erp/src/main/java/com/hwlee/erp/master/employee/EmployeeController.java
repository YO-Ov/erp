package com.hwlee.erp.master.employee;

import com.hwlee.erp.master.employee.dto.EmployeeCreateRequest;
import com.hwlee.erp.master.employee.dto.EmployeeResponse;
import com.hwlee.erp.master.employee.dto.EmployeeUpdateRequest;
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
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SALES','PURCHASING','FINANCE','HR','ADMIN')")
public class EmployeeController {

    private final EmployeeService service;

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeCreateRequest req) {
        EmployeeResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/employees/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public EmployeeResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/by-code/{code}")
    public EmployeeResponse findByCode(@PathVariable String code) {
        return service.findByCode(code);
    }

    @GetMapping
    public List<EmployeeResponse> findAll() {
        return service.findAll();
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest req) {
        return service.update(id, req);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
