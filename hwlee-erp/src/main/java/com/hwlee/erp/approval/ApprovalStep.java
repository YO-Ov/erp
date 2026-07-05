package com.hwlee.erp.approval;

import com.hwlee.erp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결재선의 한 단계 — 누가(approver) 어떤 자격으로(type) 이 문서를 처리하는지.
 *
 * <p>결재자 정보는 스냅샷({@link #approverName}, {@link #deptName})으로 함께 저장한다. 결재 후
 * 조직이 바뀌어도 "그때 누가 결재했는지" 이력이 보존되도록.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "approval_step")
public class ApprovalStep extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_id", nullable = false)
    private Approval approval;

    /** 결재선 순번. 결재(APPROVAL)는 이 순번대로 순차 진행, 합의/참조는 표시 순서용. */
    @Column(name = "step_no", nullable = false)
    private int stepNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 16)
    private ApprovalStepType type;

    /** 결재자 로그인(username=email). */
    @Column(name = "approver", nullable = false, length = 64)
    private String approver;

    /** 결재자 이름 스냅샷. */
    @Column(name = "approver_name", length = 60)
    private String approverName;

    /** 결재자 소속/직책 스냅샷(예: "영업본부장", "재무팀장"). */
    @Column(name = "dept_name", length = 100)
    private String deptName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ApprovalStepStatus status = ApprovalStepStatus.PENDING;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "comment", length = 500)
    private String comment;

    static ApprovalStep of(int stepNo, ApprovalStepType type, String approver,
                           String approverName, String deptName) {
        ApprovalStep s = new ApprovalStep();
        s.stepNo = stepNo;
        s.type = type;
        s.approver = approver;
        s.approverName = approverName;
        s.deptName = deptName;
        return s;
    }

    void assignApproval(Approval approval) {
        this.approval = approval;
    }

    void approve(LocalDateTime now, String comment) {
        this.status = ApprovalStepStatus.APPROVED;
        this.decidedAt = now;
        this.comment = comment;
    }

    void reject(LocalDateTime now, String comment) {
        this.status = ApprovalStepStatus.REJECTED;
        this.decidedAt = now;
        this.comment = comment;
    }

    /** 반송 시 미처리 상태로 되돌린다(재상신 대비). */
    void resetToPending() {
        this.status = ApprovalStepStatus.PENDING;
        this.decidedAt = null;
        this.comment = null;
    }

    boolean isPending() {
        return status == ApprovalStepStatus.PENDING;
    }
}
