package com.hwlee.erp.approval.dto;

import com.hwlee.erp.approval.ApprovalStepStatus;
import com.hwlee.erp.approval.ApprovalStepType;
import java.time.LocalDateTime;

/**
 * 결재선 한 단계의 조회 응답 — 결재 진행 타임라인 렌더에 쓰인다.
 */
public record ApprovalStepResponse(
        int stepNo,
        ApprovalStepType type,
        String typeLabel,
        String approver,
        String approverName,
        String deptName,
        ApprovalStepStatus status,
        LocalDateTime decidedAt,
        String comment
) {
}
