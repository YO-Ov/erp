package com.hwlee.erp.hr.contract;

import com.hwlee.erp.hr.contract.dto.EmploymentContractCreateRequest;
import com.hwlee.erp.hr.contract.dto.EmploymentContractResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

/**
 * 급여계약 API — 급여는 민감정보라 HR/ADMIN 전용.
 */
@RestController
@RequestMapping("/api/employment-contracts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR','ADMIN')")
public class EmploymentContractController {

    private final EmploymentContractService service;

    @PostMapping
    public ResponseEntity<EmploymentContractResponse> create(
            @Valid @RequestBody EmploymentContractCreateRequest req) {
        EmploymentContractResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/employment-contracts/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public EmploymentContractResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    public List<EmploymentContractResponse> findByEmployee(@RequestParam Long employeeId) {
        return service.findByEmployee(employeeId);
    }

    @PostMapping("/{id}/terminate")
    public EmploymentContractResponse terminate(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveTo) {
        return service.terminate(id, effectiveTo);
    }
}
