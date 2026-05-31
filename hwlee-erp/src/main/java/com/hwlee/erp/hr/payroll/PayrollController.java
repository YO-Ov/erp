package com.hwlee.erp.hr.payroll;

import com.hwlee.erp.hr.payroll.dto.PayrollRunCreateRequest;
import com.hwlee.erp.hr.payroll.dto.PayrollRunResponse;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 급여대장 API — 급여는 민감정보라 HR/ADMIN 전용.
 *
 * <p>흐름: 생성(DRAFT, 자동 계산) → 확정(인건비 전표) → 지급(지급 전표).
 */
@RestController
@RequestMapping("/api/payroll-runs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR','ADMIN')")
public class PayrollController {

    private final PayrollService service;

    @PostMapping
    public ResponseEntity<PayrollRunResponse> create(@Valid @RequestBody PayrollRunCreateRequest req) {
        PayrollRunResponse created = service.createDraft(req);
        return ResponseEntity.created(URI.create("/api/payroll-runs/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public PayrollRunResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping("/{id}/confirm")
    public PayrollRunResponse confirm(@PathVariable Long id) {
        return service.confirm(id);
    }

    @PostMapping("/{id}/pay")
    public PayrollRunResponse pay(@PathVariable Long id) {
        return service.markPaid(id);
    }
}
