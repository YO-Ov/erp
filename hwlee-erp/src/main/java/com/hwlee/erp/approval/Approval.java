package com.hwlee.erp.approval;

import com.hwlee.erp.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전자결재 문서 한 건 — 범용 결재 엔진의 중심 애그리거트.
 *
 * <p>어떤 업무 문서(견적·구매발주·지급결의…)든 이 결재 문서에 얹혀 상신→결재→완료를 탄다.
 * 결재선은 {@link ApprovalStep} 목록으로 구성되며, 결재(APPROVAL)는 순차, 합의(AGREEMENT)는
 * 병렬로 처리된다. 최종 승인 시 호출 측 서비스가 원본 문서를 전이시킨다(느슨한 콜백).
 *
 * <p>상신자 = {@code createdBy}(최초 저장자). 반송돼도 최초 상신자는 고정된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "approval_request")
public class Approval extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30, updatable = false)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 24, updatable = false)
    private ApprovalDocType docType;

    /** 원본 문서 id(예: 견적 id). 콜백·딥링크에 사용. */
    @Column(name = "ref_id", nullable = false, updatable = false)
    private Long refId;

    /** 원본 문서 번호(예: Q-20260705-001). 표시용 스냅샷. */
    @Column(name = "ref_no", length = 30, updatable = false)
    private String refNo;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 결재 금액 — 전결 규정(금액 구간) 판정 기준. */
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ApprovalStatus status = ApprovalStatus.DRAFT;

    /** 현재 진행 중인 순차 결재(APPROVAL) 단계의 stepNo. 상신 전/완료 후 0. */
    @Column(name = "current_step", nullable = false)
    private int currentStep;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    /** 마지막 반송 사유(반송 시 기록, 재상신 시 참고). */
    @Column(name = "return_reason", length = 500)
    private String returnReason;

    @OneToMany(mappedBy = "approval", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNo ASC")
    private List<ApprovalStep> steps = new ArrayList<>();

    public static Approval create(String number, ApprovalDocType docType, Long refId,
                                  String refNo, String title, BigDecimal amount) {
        if (number == null || number.isBlank()) throw new IllegalArgumentException("number 는 비어 있을 수 없다.");
        if (docType == null) throw new IllegalArgumentException("docType 은 null 일 수 없다.");
        if (refId == null) throw new IllegalArgumentException("refId 는 null 일 수 없다.");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title 은 필수입니다.");
        Approval a = new Approval();
        a.number = number;
        a.docType = docType;
        a.refId = refId;
        a.refNo = refNo;
        a.title = title;
        a.amount = amount == null ? BigDecimal.ZERO : amount;
        return a;
    }

    public void addStep(ApprovalStep step) {
        step.assignApproval(this);
        this.steps.add(step);
    }

    /**
     * 상신 — DRAFT → PENDING. 결재선(steps)이 이미 채워져 있어야 한다.
     * 반송(DRAFT 복귀) 후 재상신에도 그대로 쓰인다.
     */
    public void submit(LocalDateTime now) {
        if (status != ApprovalStatus.DRAFT)
            throw new IllegalStateException("작성(DRAFT) 상태만 상신할 수 있습니다. 현재: " + status);
        if (steps.stream().noneMatch(s -> s.getType() == ApprovalStepType.APPROVAL))
            throw new IllegalStateException("결재(승인) 단계가 없는 결재선은 상신할 수 없습니다.");
        this.status = ApprovalStatus.PENDING;
        this.requestedAt = now;
        this.currentStep = firstPendingApprovalStepNo();
    }

    /**
     * 한 결재자의 처리 — 승인/반려/반송. 처리 가능한 단계를 찾아 반영하고
     * 문서 상태를 갱신한다. 처리한 단계를 돌려준다(알림 라우팅용).
     */
    public ApprovalStep act(String username, ApprovalAction action, String comment, LocalDateTime now) {
        if (status != ApprovalStatus.PENDING)
            throw new IllegalStateException("진행 중(PENDING) 문서만 결재할 수 있습니다. 현재: " + status);
        ApprovalStep step = findActionableStep(username);
        if (step == null)
            throw new IllegalStateException("현재 결재/합의할 차례가 아닙니다: " + username);
        switch (action) {
            case APPROVE -> {
                step.approve(now, comment);
                if (step.getType() == ApprovalStepType.APPROVAL) currentStep = firstPendingApprovalStepNo();
                if (isAllCleared()) {
                    this.status = ApprovalStatus.APPROVED;
                    this.decidedAt = now;
                    this.currentStep = 0;
                }
            }
            case REJECT -> {
                step.reject(now, comment);
                this.status = ApprovalStatus.REJECTED;
                this.decidedAt = now;
                this.currentStep = 0;
            }
            case RETURN -> {
                if (step.getType() != ApprovalStepType.APPROVAL)
                    throw new IllegalStateException("합의/참조 단계는 반송할 수 없습니다. 반려만 가능합니다.");
                this.returnReason = comment;
                this.status = ApprovalStatus.DRAFT;
                this.currentStep = 0;
                steps.forEach(ApprovalStep::resetToPending);
            }
        }
        return step;
    }

    /** 상신자 회수 — 아직 아무도 처리하지 않았을 때만. */
    public void withdraw() {
        if (status != ApprovalStatus.PENDING)
            throw new IllegalStateException("진행 중(PENDING) 문서만 회수할 수 있습니다. 현재: " + status);
        boolean anyDecided = steps.stream().anyMatch(s -> s.getStatus() != ApprovalStepStatus.PENDING);
        if (anyDecided)
            throw new IllegalStateException("이미 결재가 진행된 문서는 회수할 수 없습니다.");
        this.status = ApprovalStatus.WITHDRAWN;
        this.currentStep = 0;
    }

    /** 현재 이 사용자가 처리할 수 있는 단계 — 순차 결재 우선, 없으면 병렬 합의. */
    private ApprovalStep findActionableStep(String username) {
        ApprovalStep seq = steps.stream()
                .filter(s -> s.getType() == ApprovalStepType.APPROVAL
                        && s.getStepNo() == currentStep && s.isPending()
                        && s.getApprover().equals(username))
                .findFirst().orElse(null);
        if (seq != null) return seq;
        return steps.stream()
                .filter(s -> s.getType() == ApprovalStepType.AGREEMENT
                        && s.isPending() && s.getApprover().equals(username))
                .findFirst().orElse(null);
    }

    private int firstPendingApprovalStepNo() {
        return steps.stream()
                .filter(s -> s.getType() == ApprovalStepType.APPROVAL && s.isPending())
                .mapToInt(ApprovalStep::getStepNo)
                .min().orElse(0);
    }

    /** 모든 결재(APPROVAL)·합의(AGREEMENT) 단계가 승인/동의됐는가(참조는 무관). */
    private boolean isAllCleared() {
        return steps.stream()
                .filter(s -> s.getType() != ApprovalStepType.REFERENCE)
                .allMatch(s -> s.getStatus() == ApprovalStepStatus.APPROVED);
    }
}
