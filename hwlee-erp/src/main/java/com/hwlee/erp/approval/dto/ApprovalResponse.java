package com.hwlee.erp.approval.dto;

import com.hwlee.erp.approval.ApprovalDocType;
import com.hwlee.erp.approval.ApprovalStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 결재 문서 조회 응답.
 *
 * @param myTurn 조회자(현재 로그인 사용자)가 지금 이 문서를 처리할 차례인지 — 결재/합의 버튼 노출용.
 */
public record ApprovalResponse(
        Long id,
        String number,
        ApprovalDocType docType,
        String docTypeLabel,
        Long refId,
        String refNo,
        String docLink,
        String title,
        BigDecimal amount,
        ApprovalStatus status,
        int currentStep,
        String requester,
        LocalDateTime requestedAt,
        LocalDateTime decidedAt,
        String returnReason,
        boolean myTurn,
        List<ApprovalStepResponse> steps
) {
}
