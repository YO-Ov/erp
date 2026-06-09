package com.hwlee.erp.pp.integration.mes;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ERP↔MES 정합성 검증 API. */
@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCTION','ADMIN')")
public class ReconciliationController {

    private final ReconciliationService service;

    @GetMapping("/reconciliation")
    public ReconciliationResponse reconcile() {
        return service.reconcile();
    }
}
