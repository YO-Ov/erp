package com.hwlee.erp.approval;

import com.hwlee.erp.approval.dto.ApprovalActionRequest;
import com.hwlee.erp.approval.dto.ApprovalResponse;
import com.hwlee.erp.approval.dto.ApprovalStepResponse;
import com.hwlee.erp.approval.dto.ApprovalSubmitCommand;
import com.hwlee.erp.approval.event.ApprovalApprovedEvent;
import com.hwlee.erp.approval.event.ApprovalRejectedEvent;
import com.hwlee.erp.common.code.TransactionNumberGenerator;
import com.hwlee.erp.notification.NotificationService;
import com.hwlee.erp.notification.NotificationType;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전자결재 서비스 — 상신, 결재(승인/반려/반송), 회수, 조회, 알림 라우팅.
 *
 * <p>상신 시 전결 규정으로 결재선을 자동 구성하고 첫 결재자·합의자에게 알림한다. 각 처리 단계마다
 * 다음 결재자에게, 최종 승인/반려/반송 시 상신자에게 알림한다. 최종 승인 시 원본 문서 전이는
 * {@link ApprovalApprovedEvent} 로 위임한다(견적 → SENT 등).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalService {

    static final String LINK = "/approvals";

    private final ApprovalRepository repository;
    private final ApprovalRuleRepository ruleRepository;
    private final ApprovalLineResolver lineResolver;
    private final NotificationService notificationService;
    private final TransactionNumberGenerator numberGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /** 상신 — 전결 규정으로 결재선을 만들어 결재를 시작한다. 첫 결재자·합의자에게 알림. */
    @Transactional
    public ApprovalResponse submit(ApprovalSubmitCommand cmd) {
        // 진행 중(PENDING)이거나 이미 승인된 결재가 있으면 중복 상신을 막는다.
        repository.findByDocTypeAndRefIdOrderByIdDesc(cmd.docType(), cmd.refId()).stream()
                .filter(a -> a.getStatus() == ApprovalStatus.PENDING || a.getStatus() == ApprovalStatus.APPROVED)
                .findFirst()
                .ifPresent(a -> {
                    throw new IllegalStateException(
                            "이미 진행 중이거나 승인된 결재가 있습니다: " + a.getNumber() + " (" + a.getStatus() + ")");
                });

        ApprovalRule rule = lineResolver.pickRule(
                ruleRepository.findByDocTypeOrderByMinAmountAsc(cmd.docType()), cmd.amount());
        if (rule == null)
            throw new IllegalStateException(cmd.docType().label() + " 문서의 전결 규정이 없습니다. 관리자에게 문의하세요.");

        List<ApprovalStep> steps = lineResolver.resolve(cmd.requester(), rule);
        if (steps.stream().noneMatch(s -> s.getType() == ApprovalStepType.APPROVAL))
            throw new IllegalStateException("결재선을 구성할 수 없습니다. 조직의 부서장 지정을 확인하세요.");

        String number = numberGenerator.nextApprovalNumber(LocalDate.now(clock));
        Approval approval = Approval.create(number, cmd.docType(), cmd.refId(), cmd.refNo(), cmd.title(), cmd.amount());
        steps.forEach(approval::addStep);
        approval.submit(LocalDateTime.now(clock));
        Approval saved = repository.save(approval);

        notifyReviewersOnSubmit(saved);
        log.info("결재 상신: {} {} (금액 {}, 결재선 {}단계)", saved.getNumber(), cmd.title(),
                money(cmd.amount()), steps.size());
        return toResponse(saved, cmd.requester());
    }

    @Transactional
    public ApprovalResponse approve(Long id, ApprovalActionRequest req, String actor) {
        Approval a = getOrThrow(id);
        a.act(actor, ApprovalAction.APPROVE, comment(req), LocalDateTime.now(clock));
        routeAfterDecision(a, actor);
        return toResponse(a, actor);
    }

    @Transactional
    public ApprovalResponse reject(Long id, ApprovalActionRequest req, String actor) {
        Approval a = getOrThrow(id);
        a.act(actor, ApprovalAction.REJECT, comment(req), LocalDateTime.now(clock));
        routeAfterDecision(a, actor);
        return toResponse(a, actor);
    }

    /** 반송 — 상신자에게 되돌려 수정·재상신하게 한다. */
    @Transactional
    public ApprovalResponse returnToDrafter(Long id, ApprovalActionRequest req, String actor) {
        Approval a = getOrThrow(id);
        a.act(actor, ApprovalAction.RETURN, comment(req), LocalDateTime.now(clock));
        routeAfterDecision(a, actor);
        return toResponse(a, actor);
    }

    /** 상신자 회수 — 본인만, 아직 아무도 처리하지 않았을 때. */
    @Transactional
    public ApprovalResponse withdraw(Long id, String actor) {
        Approval a = getOrThrow(id);
        if (!a.getCreatedBy().equals(actor))
            throw new IllegalStateException("본인이 상신한 결재만 회수할 수 있습니다.");
        a.withdraw();
        return toResponse(a, actor);
    }

    /** 재상신 — 반송(DRAFT)된 자기 결재를 다시 올린다. 결재선은 그대로 재사용한다. */
    @Transactional
    public ApprovalResponse resubmit(Long id, String actor) {
        Approval a = getOrThrow(id);
        if (!a.getCreatedBy().equals(actor))
            throw new IllegalStateException("본인이 상신한 결재만 재상신할 수 있습니다.");
        a.submit(LocalDateTime.now(clock));
        notifyReviewersOnSubmit(a);
        return toResponse(a, actor);
    }

    public ApprovalResponse findById(Long id, String viewer) {
        return toResponse(getOrThrow(id), viewer);
    }

    /** 한 원본 문서(예: 견적)의 최신 결재 — 없으면 null. 문서 화면의 진행 상태 표시용. */
    public ApprovalResponse findLatestForDoc(ApprovalDocType docType, Long refId, String viewer) {
        return repository.findByDocTypeAndRefIdOrderByIdDesc(docType, refId).stream()
                .findFirst().map(a -> toResponse(a, viewer)).orElse(null);
    }

    /**
     * 여러 원본 문서의 "최신 결재 상태" 맵 — 문서 목록 화면이 결재 상태 배지를 배치로 채울 때 쓴다.
     * refId → 최신 결재 상태. 결재가 없는 문서는 맵에 없다.
     */
    public Map<Long, ApprovalStatus> latestStatusByRef(ApprovalDocType docType, Collection<Long> refIds) {
        if (refIds == null || refIds.isEmpty()) return Map.of();
        Map<Long, ApprovalStatus> map = new HashMap<>();
        // Desc 정렬이라 각 refId 의 첫 등장이 최신 결재.
        for (Approval a : repository.findByDocTypeAndRefIdInOrderByIdDesc(docType, refIds))
            map.putIfAbsent(a.getRefId(), a.getStatus());
        return map;
    }

    /** 상신함 — 내가 올린 결재. */
    public Page<ApprovalResponse> outbox(String username, Pageable pageable) {
        return repository.findByCreatedByOrderByIdDesc(username, pageable).map(a -> toResponse(a, username));
    }

    /** 결재함 — 내가 처리할 차례인 결재. */
    public Page<ApprovalResponse> inbox(String username, Pageable pageable) {
        return repository.findInboxOf(username, pageable).map(a -> toResponse(a, username));
    }

    // ── 알림 라우팅 ───────────────────────────────────────────────

    /** 상신/재상신 직후 — 현재 결재 차례인 사람, 모든 합의자, 참조자에게 통보. */
    private void notifyReviewersOnSubmit(Approval a) {
        for (ApprovalStep s : a.getSteps()) {
            boolean firstApprover = s.getType() == ApprovalStepType.APPROVAL && s.getStepNo() == a.getCurrentStep();
            boolean agreement = s.getType() == ApprovalStepType.AGREEMENT;
            if (firstApprover || agreement) {
                notificationService.notifyUser(s.getApprover(), NotificationType.APPROVAL_REQUESTED,
                        (agreement ? "합의 요청" : "결재 요청"),
                        String.format("[%s] %s — %s (%s)", a.getNumber(), a.getTitle(),
                                a.getDocType().label(), money(a.getAmount())),
                        LINK);
            } else if (s.getType() == ApprovalStepType.REFERENCE) {
                // 참조자는 결재권 없이 열람만 — 통보만 보낸다. 딥링크로 해당 문서를 바로 열람.
                notificationService.notifyUser(s.getApprover(), NotificationType.APPROVAL_REFERENCED,
                        "참조 통보",
                        String.format("[%s] %s — %s (%s) · 참조", a.getNumber(), a.getTitle(),
                                a.getDocType().label(), money(a.getAmount())),
                        LINK + "?id=" + a.getId());
            }
        }
    }

    /** 처리 후 — 상태에 따라 다음 결재자 또는 상신자에게 통보. */
    private void routeAfterDecision(Approval a, String actor) {
        switch (a.getStatus()) {
            case APPROVED -> {
                notificationService.notifyUser(a.getCreatedBy(), NotificationType.APPROVAL_APPROVED,
                        "결재 승인 완료",
                        String.format("[%s] %s 이(가) 최종 승인되었습니다.", a.getNumber(), a.getTitle()), LINK);
                eventPublisher.publishEvent(new ApprovalApprovedEvent(
                        a.getDocType(), a.getRefId(), a.getRefNo(), a.getNumber(), actor));
                log.info("결재 최종 승인: {} {}", a.getNumber(), a.getTitle());
            }
            case REJECTED -> {
                String reason = a.getSteps().stream()
                        .filter(s -> s.getStatus() == ApprovalStepStatus.REJECTED)
                        .map(ApprovalStep::getComment).filter(c -> c != null && !c.isBlank())
                        .findFirst().orElse(null);
                notificationService.notifyUser(a.getCreatedBy(), NotificationType.APPROVAL_REJECTED,
                        "결재 반려됨",
                        String.format("[%s] %s 이(가) 반려되었습니다.%s", a.getNumber(), a.getTitle(),
                                reason != null ? " 사유: " + reason : ""),
                        LINK);
                eventPublisher.publishEvent(new ApprovalRejectedEvent(
                        a.getDocType(), a.getRefId(), a.getRefNo(), a.getNumber(), actor, reason));
            }
            case DRAFT -> notificationService.notifyUser(a.getCreatedBy(), NotificationType.APPROVAL_RETURNED,
                    "결재 반송됨 — 수정 후 재상신하세요",
                    String.format("[%s] %s 이(가) 반송되었습니다.%s", a.getNumber(), a.getTitle(),
                            a.getReturnReason() != null && !a.getReturnReason().isBlank()
                                    ? " 사유: " + a.getReturnReason() : ""),
                    LINK);
            case PENDING -> notifyNextApprover(a);
            default -> { /* WITHDRAWN 등: 알림 없음 */ }
        }
    }

    /** 다음 순차 결재자에게 통보(승인이 진행돼 차례가 넘어간 경우). */
    private void notifyNextApprover(Approval a) {
        if (a.getCurrentStep() <= 0) return; // 합의만 남은 상태 등
        a.getSteps().stream()
                .filter(s -> s.getType() == ApprovalStepType.APPROVAL
                        && s.getStepNo() == a.getCurrentStep() && s.isPending())
                .findFirst()
                .ifPresent(s -> notificationService.notifyUser(s.getApprover(), NotificationType.APPROVAL_REQUESTED,
                        "결재 요청",
                        String.format("[%s] %s — %s (%s)", a.getNumber(), a.getTitle(),
                                a.getDocType().label(), money(a.getAmount())),
                        LINK));
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────

    private Approval getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Approval not found: id=" + id));
    }

    private static String comment(ApprovalActionRequest req) {
        return req == null ? null : req.comment();
    }

    private static String money(BigDecimal v) {
        return v == null ? "-" : String.format("%,d원", v.longValue());
    }

    private ApprovalResponse toResponse(Approval a, String viewer) {
        List<ApprovalStepResponse> steps = a.getSteps().stream()
                .map(s -> new ApprovalStepResponse(
                        s.getStepNo(), s.getType(), s.getType().label(),
                        s.getApprover(), s.getApproverName(), s.getDeptName(),
                        s.getStatus(), s.getDecidedAt(), s.getComment()))
                .toList();
        return new ApprovalResponse(
                a.getId(), a.getNumber(), a.getDocType(), a.getDocType().label(),
                a.getRefId(), a.getRefNo(), a.getDocType().linkTo(a.getRefId()),
                a.getTitle(), a.getAmount(), a.getStatus(), a.getCurrentStep(),
                a.getCreatedBy(), a.getRequestedAt(), a.getDecidedAt(), a.getReturnReason(),
                isMyTurn(a, viewer), steps);
    }

    /** 조회자가 지금 이 문서를 처리할 차례인가 — 화면의 결재 버튼 노출용. */
    private boolean isMyTurn(Approval a, String viewer) {
        if (a.getStatus() != ApprovalStatus.PENDING || viewer == null) return false;
        return a.getSteps().stream().anyMatch(s -> s.isPending() && s.getApprover().equals(viewer)
                && (s.getType() == ApprovalStepType.AGREEMENT
                || (s.getType() == ApprovalStepType.APPROVAL && s.getStepNo() == a.getCurrentStep())));
    }
}
