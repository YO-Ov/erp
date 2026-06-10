package com.hwlee.erp.fi.credit;

import com.hwlee.erp.common.code.TransactionNumberGenerator;
import com.hwlee.erp.fi.credit.dto.CreditLimitDecisionRequest;
import com.hwlee.erp.fi.credit.dto.CreditLimitRequestCreateRequest;
import com.hwlee.erp.fi.credit.dto.CreditLimitRequestResponse;
import com.hwlee.erp.master.customer.Customer;
import com.hwlee.erp.master.customer.CustomerRepository;
import com.hwlee.erp.notification.NotificationService;
import com.hwlee.erp.notification.NotificationType;
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

    static final String LINK = "/fi/credit-limit-requests";

    private final CreditLimitRequestRepository repository;
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;
    private final TransactionNumberGenerator numberGenerator;
    private final Clock clock;

    /** 영업이 여신 상향 요청 제출 → 재무팀(FINANCE)에게 알림. */
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

        notificationService.notifyRole("FINANCE", NotificationType.CREDIT_REQUEST_SUBMITTED,
                "여신 상향 요청",
                String.format("%s 고객 한도 상향 요청 (%s → %s). 사유: %s",
                        customer.getName(), money(saved.getCurrentLimit()),
                        money(saved.getRequestedLimit()), req.reason()),
                LINK);
        return toResponse(saved);
    }

    /** 재무 승인 → 고객 한도 상향 + 요청자(영업)에게 알림. */
    @Transactional
    public CreditLimitRequestResponse approve(Long id, CreditLimitDecisionRequest decision, String decidedBy) {
        CreditLimitRequest r = getOrThrow(id);
        r.approve(decidedBy, decision == null ? null : decision.note(), LocalDateTime.now(clock));
        r.getCustomer().changeCreditLimit(r.getRequestedLimit()); // 실제 한도 반영

        notificationService.notifyUser(r.getCreatedBy(), NotificationType.CREDIT_REQUEST_APPROVED,
                "여신 상향 승인됨",
                String.format("%s 고객 한도가 %s 로 상향 승인되었습니다.",
                        r.getCustomer().getName(), money(r.getRequestedLimit())),
                LINK);
        log.info("여신 상향 승인: {} {} → {}", r.getCustomer().getName(),
                money(r.getCurrentLimit()), money(r.getRequestedLimit()));
        return toResponse(r);
    }

    /** 재무 거부 → 요청자(영업)에게 알림. 고객 한도 변화 없음. */
    @Transactional
    public CreditLimitRequestResponse reject(Long id, CreditLimitDecisionRequest decision, String decidedBy) {
        CreditLimitRequest r = getOrThrow(id);
        r.reject(decidedBy, decision == null ? null : decision.note(), LocalDateTime.now(clock));

        notificationService.notifyUser(r.getCreatedBy(), NotificationType.CREDIT_REQUEST_REJECTED,
                "여신 상향 거부됨",
                String.format("%s 고객 한도 상향 요청이 거부되었습니다.%s",
                        r.getCustomer().getName(),
                        (decision != null && decision.note() != null && !decision.note().isBlank())
                                ? " 사유: " + decision.note() : ""),
                LINK);
        return toResponse(r);
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
