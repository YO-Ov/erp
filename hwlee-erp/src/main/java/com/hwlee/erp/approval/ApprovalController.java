package com.hwlee.erp.approval;

import com.hwlee.erp.approval.dto.ApprovalActionRequest;
import com.hwlee.erp.approval.dto.ApprovalResponse;
import java.security.Principal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * 전자결재 API — 상신함/결재함 조회와 결재 처리(승인/반려/반송/회수/재상신).
 *
 * <p>모든 로그인 사용자가 자기 상신함·결재함을 본다. 처리 권한은 결재선(단계별 결재자)로
 * 서버가 검증하므로 역할 기반 제약은 두지 않는다. 상신은 각 문서 모듈(예: 견적)이 담당한다.
 */
@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ApprovalController {

    private final ApprovalService service;

    @GetMapping("/inbox")
    public Page<ApprovalResponse> inbox(Principal principal, Pageable pageable) {
        return service.inbox(principal.getName(), pageable);
    }

    /** 상신함. dateFrom·dateTo 를 주면 상신일 기준으로 그 기간만 (예: "이번 달 상신한 결재"). */
    @GetMapping("/outbox")
    public Page<ApprovalResponse> outbox(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable
    ) {
        return service.outbox(principal.getName(), dateFrom, dateTo, pageable);
    }

    @GetMapping("/{id}")
    public ApprovalResponse findById(@PathVariable Long id, Principal principal) {
        return service.findById(id, principal.getName());
    }

    /** 원본 문서의 최신 결재 — 있으면 200+본문, 없으면 204. 문서 화면 진행 상태 표시용. */
    @GetMapping("/for")
    public ResponseEntity<ApprovalResponse> forDoc(@RequestParam ApprovalDocType docType,
                                                   @RequestParam Long refId, Principal principal) {
        ApprovalResponse r = service.findLatestForDoc(docType, refId, principal.getName());
        return r == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(r);
    }

    /** 여러 문서의 최신 결재 상태 맵(refId→status) — 문서 목록의 결재 상태 배지를 배치로 채운다. */
    @GetMapping("/status")
    public java.util.Map<Long, ApprovalStatus> statuses(@RequestParam ApprovalDocType docType,
                                                        @RequestParam java.util.List<Long> refIds) {
        return service.latestStatusByRef(docType, refIds);
    }

    @PostMapping("/{id}/approve")
    public ApprovalResponse approve(@PathVariable Long id,
                                    @RequestBody(required = false) ApprovalActionRequest req,
                                    Principal principal) {
        return service.approve(id, req, principal.getName());
    }

    @PostMapping("/{id}/reject")
    public ApprovalResponse reject(@PathVariable Long id,
                                   @RequestBody(required = false) ApprovalActionRequest req,
                                   Principal principal) {
        return service.reject(id, req, principal.getName());
    }

    @PostMapping("/{id}/return")
    public ApprovalResponse returnToDrafter(@PathVariable Long id,
                                            @RequestBody(required = false) ApprovalActionRequest req,
                                            Principal principal) {
        return service.returnToDrafter(id, req, principal.getName());
    }

    @PostMapping("/{id}/withdraw")
    public ApprovalResponse withdraw(@PathVariable Long id, Principal principal) {
        return service.withdraw(id, principal.getName());
    }

    @PostMapping("/{id}/resubmit")
    public ApprovalResponse resubmit(@PathVariable Long id, Principal principal) {
        return service.resubmit(id, principal.getName());
    }
}
