package com.hwlee.erp.sd.quotation;

import static com.hwlee.erp.sd.quotation.QuotationSpecifications.customerIdEquals;
import static com.hwlee.erp.sd.quotation.QuotationSpecifications.issuedFrom;
import static com.hwlee.erp.sd.quotation.QuotationSpecifications.issuedTo;
import static com.hwlee.erp.sd.quotation.QuotationSpecifications.statusEquals;
import static org.springframework.data.jpa.domain.Specification.where;

import com.hwlee.erp.approval.dto.ApprovalResponse;
import com.hwlee.erp.sd.quotation.dto.QuotationBulkRequest;
import com.hwlee.erp.sd.quotation.dto.QuotationBulkResponse;
import com.hwlee.erp.sd.quotation.dto.QuotationCreateRequest;
import com.hwlee.erp.sd.quotation.dto.QuotationResponse;
import com.hwlee.erp.sd.quotation.dto.QuotationUpdateRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quotations")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SALES','ADMIN')")
public class QuotationController {

    private final QuotationService service;

    @PostMapping
    public ResponseEntity<QuotationResponse> create(@Valid @RequestBody QuotationCreateRequest req) {
        QuotationResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/quotations/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public QuotationResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    public Page<QuotationResponse> search(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) QuotationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable
    ) {
        return service.search(
                where(customerIdEquals(customerId))
                        .and(statusEquals(status))
                        .and(issuedFrom(dateFrom))
                        .and(issuedTo(dateTo)),
                pageable
        );
    }

    @PutMapping("/{id}")
    public QuotationResponse update(@PathVariable Long id, @Valid @RequestBody QuotationUpdateRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/bulk")
    public QuotationBulkResponse bulk(@Valid @RequestBody QuotationBulkRequest req) {
        return service.bulk(req);
    }

    @PostMapping("/{id}/send")
    public QuotationResponse send(@PathVariable Long id) {
        return service.send(id);
    }

    /** 결재 상신 — 작성 중 견적을 전자결재에 올린다. 최종 승인 시 자동 발송(SENT). */
    @PostMapping("/{id}/submit-approval")
    public ApprovalResponse submitForApproval(@PathVariable Long id, Principal principal) {
        return service.submitForApproval(id, principal.getName());
    }

    @PostMapping("/{id}/accept")
    public QuotationResponse accept(@PathVariable Long id) {
        return service.accept(id);
    }

    @PostMapping("/{id}/cancel")
    public QuotationResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
