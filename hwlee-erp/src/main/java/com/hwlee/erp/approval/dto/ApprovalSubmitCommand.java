package com.hwlee.erp.approval.dto;

import com.hwlee.erp.approval.ApprovalDocType;
import java.math.BigDecimal;

/**
 * 결재 상신 명령 — 각 문서 모듈(예: 견적)이 원본 문서를 조회해 채워 넘긴다.
 * 결재 엔진은 원본 문서를 직접 알지 않고 이 스냅샷만으로 결재선을 구성한다.
 *
 * @param requester 상신자 username(=email)
 */
public record ApprovalSubmitCommand(
        ApprovalDocType docType,
        Long refId,
        String refNo,
        String title,
        BigDecimal amount,
        String requester
) {
}
