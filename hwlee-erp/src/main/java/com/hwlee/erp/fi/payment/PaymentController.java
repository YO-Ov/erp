package com.hwlee.erp.fi.payment;

import static com.hwlee.erp.fi.payment.PaymentSpecifications.customerIdEquals;
import static com.hwlee.erp.fi.payment.PaymentSpecifications.dateFrom;
import static com.hwlee.erp.fi.payment.PaymentSpecifications.dateTo;
import static com.hwlee.erp.fi.payment.PaymentSpecifications.typeEquals;
import static com.hwlee.erp.fi.payment.PaymentSpecifications.vendorIdEquals;
import static org.springframework.data.jpa.domain.Specification.where;

import com.hwlee.erp.approval.dto.ApprovalResponse;
import com.hwlee.erp.fi.payment.dto.PaymentCreateRequest;
import com.hwlee.erp.fi.payment.dto.PaymentResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
public class PaymentController {

    private final PaymentService service;

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentCreateRequest req) {
        PaymentResponse created = service.createAndPost(req);
        return ResponseEntity.created(URI.create("/api/payments/" + created.id())).body(created);
    }

    /** 결재용 초안 등록 — 전기하지 않고 DRAFT 로만 저장(지급 결재 상신 대상). */
    @PostMapping("/draft")
    public ResponseEntity<PaymentResponse> createDraft(@Valid @RequestBody PaymentCreateRequest req) {
        PaymentResponse created = service.createDraft(req);
        return ResponseEntity.created(URI.create("/api/payments/" + created.id())).body(created);
    }

    /** 지급(출금) 결재 상신 — 최종 승인 시 자동 전기(POSTED)+분개. */
    @PostMapping("/{id}/submit-approval")
    public ApprovalResponse submitForApproval(@PathVariable Long id, Principal principal) {
        return service.submitForApproval(id, principal.getName());
    }

    @GetMapping("/{id}")
    public PaymentResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/by-number/{number}")
    public PaymentResponse findByNumber(@PathVariable String number) {
        return service.findByNumber(number);
    }

    @GetMapping
    public Page<PaymentResponse> search(
            @RequestParam(required = false) PaymentType type,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable
    ) {
        return service.search(
                where(typeEquals(type))
                        .and(customerIdEquals(customerId))
                        .and(vendorIdEquals(vendorId))
                        .and(dateFrom(dateFrom))
                        .and(dateTo(dateTo)),
                pageable
        );
    }
}
