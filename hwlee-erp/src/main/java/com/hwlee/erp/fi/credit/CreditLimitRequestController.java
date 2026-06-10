package com.hwlee.erp.fi.credit;

import com.hwlee.erp.fi.credit.dto.CreditLimitDecisionRequest;
import com.hwlee.erp.fi.credit.dto.CreditLimitRequestCreateRequest;
import com.hwlee.erp.fi.credit.dto.CreditLimitRequestResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * 여신 상향 요청 API. 조회는 영업·재무 공용, 요청은 영업, 결정(승인/거부)은 재무.
 */
@RestController
@RequestMapping("/api/credit-limit-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES','FINANCE','ADMIN')")
public class CreditLimitRequestController {

    private final CreditLimitRequestService service;

    @GetMapping
    public Page<CreditLimitRequestResponse> search(
            @RequestParam(required = false) CreditLimitRequestStatus status,
            Pageable pageable) {
        return service.search(status, pageable);
    }

    @GetMapping("/{id}")
    public CreditLimitRequestResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    /** 한 고객의 검토 대기 요청 조회 — 있으면 200+본문, 없으면 204. 화면 중복 차단/안내용. */
    @GetMapping("/pending")
    public ResponseEntity<CreditLimitRequestResponse> pending(@RequestParam Long customerId) {
        CreditLimitRequestResponse pending = service.findPendingByCustomer(customerId);
        return pending == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(pending);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SALES','ADMIN')")
    public CreditLimitRequestResponse create(@Valid @RequestBody CreditLimitRequestCreateRequest req) {
        return service.create(req);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public CreditLimitRequestResponse approve(@PathVariable Long id,
                                              @RequestBody(required = false) CreditLimitDecisionRequest decision,
                                              Principal principal) {
        return service.approve(id, decision, principal.getName());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public CreditLimitRequestResponse reject(@PathVariable Long id,
                                             @RequestBody(required = false) CreditLimitDecisionRequest decision,
                                             Principal principal) {
        return service.reject(id, decision, principal.getName());
    }
}
