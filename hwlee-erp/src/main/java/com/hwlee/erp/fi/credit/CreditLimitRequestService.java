package com.hwlee.erp.fi.credit;

import com.hwlee.erp.approval.ApprovalDocType;
import com.hwlee.erp.approval.ApprovalService;
import com.hwlee.erp.approval.dto.ApprovalSubmitCommand;
import com.hwlee.erp.common.code.TransactionNumberGenerator;
import com.hwlee.erp.fi.credit.dto.CreditLimitRequestCreateRequest;
import com.hwlee.erp.fi.credit.dto.CreditLimitRequestResponse;
import com.hwlee.erp.master.customer.Customer;
import com.hwlee.erp.master.customer.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 여신(신용한도) 상향 요청 서비스 — 영업의 요청 접수, 재무의 승인/거부, 알림 라우팅.
 *
 * <p>요청 시 FINANCE 역할 전체에게 알림(누가 처리할지 미정), 결정 시 요청자(영업)에게 결과 알림.
 * 승인은 고객 마스터의 한도를 실제로 올린다(감사 로그 자동 기록).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreditLimitRequestService {

    private final CreditLimitRequestRepository repository;
    private final CustomerRepository customerRepository;
    private final ApprovalService approvalService;
    private final TransactionNumberGenerator numberGenerator;
    private final Clock clock;

    /**
     * 영업이 여신 상향 요청 제출 → 전자결재로 상신(재무팀장 결재선). 승인/반려는 결재함에서 처리되며,
     * 그 결과가 {@code CreditApprovalListener} 콜백으로 이 요청의 상태에 반영된다.
     */
    @Transactional
    public CreditLimitRequestResponse create(CreditLimitRequestCreateRequest req) {
        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: id=" + req.customerId()));
        // 중복 방지: 같은 고객에 이미 검토 대기(PENDING) 요청이 있으면 거부한다.
        if (repository.existsByCustomerIdAndStatus(customer.getId(), CreditLimitRequestStatus.PENDING))
            throw new IllegalStateException(
                    "이미 검토 중인 여신 상향 요청이 있습니다. 재무의 결정(승인/거부) 후 다시 요청하세요.");
        String number = numberGenerator.nextCreditLimitRequestNumber(LocalDate.now(clock));
        CreditLimitRequest saved = repository.save(CreditLimitRequest.submit(
                number, customer, customer.getCreditLimit(), req.requestedLimit(), req.reason()));

        approvalService.submit(new ApprovalSubmitCommand(
                ApprovalDocType.CREDIT_LIMIT, saved.getId(), saved.getNumber(),
                "여신 상향 · " + customer.getName() + " (" + money(saved.getCurrentLimit())
                        + " → " + money(saved.getRequestedLimit()) + ")",
                saved.getRequestedLimit(), saved.getCreatedBy()));
        return toResponse(saved);
    }

    /** 결재 최종 승인 콜백 — 요청을 APPROVED 로 확정하고 고객 한도를 실제로 상향한다. */
    @Transactional
    public void applyApproval(Long id, String decidedBy) {
        CreditLimitRequest r = getOrThrow(id);
        r.approve(decidedBy, null, LocalDateTime.now(clock));
        r.getCustomer().changeCreditLimit(r.getRequestedLimit());
        log.info("여신 상향 승인(결재): {} {} → {}", r.getCustomer().getName(),
                money(r.getCurrentLimit()), money(r.getRequestedLimit()));
    }

    /** 결재 반려 콜백 — 요청을 REJECTED 로 종결한다. 고객 한도 변화 없음. */
    @Transactional
    public void applyRejection(Long id, String decidedBy, String reason) {
        getOrThrow(id).reject(decidedBy, reason, LocalDateTime.now(clock));
    }

    public CreditLimitRequestResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    /** 한 고객의 검토 대기(PENDING) 요청 — 없으면 null. 화면이 "검토 중" 안내/중복 차단에 쓴다. */
    public CreditLimitRequestResponse findPendingByCustomer(Long customerId) {
        return repository.findFirstByCustomerIdAndStatusOrderByIdDesc(customerId, CreditLimitRequestStatus.PENDING)
                .map(this::toResponse).orElse(null);
    }

    public Page<CreditLimitRequestResponse> search(CreditLimitRequestStatus status, Pageable pageable) {
        Specification<CreditLimitRequest> spec = (root, query, cb) -> cb.conjunction();
        if (status != null)
            spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        return repository.findAll(spec, pageable).map(this::toResponse);
    }

    private CreditLimitRequest getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CreditLimitRequest not found: id=" + id));
    }

    private static String money(BigDecimal v) {
        return v == null ? "-" : String.format("%,d원", v.longValue());
    }

    private CreditLimitRequestResponse toResponse(CreditLimitRequest r) {
        return new CreditLimitRequestResponse(
                r.getId(), r.getNumber(),
                r.getCustomer().getId(), r.getCustomer().getName(),
                r.getCurrentLimit(), r.getRequestedLimit(), r.getReason(), r.getStatus(),
                r.getCreatedBy(), r.getCreatedAt(),
                r.getDecidedBy(), r.getDecidedAt(), r.getDecisionNote());
    }
}
