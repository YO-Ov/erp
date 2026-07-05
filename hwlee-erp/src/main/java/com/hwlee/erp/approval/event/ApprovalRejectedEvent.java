package com.hwlee.erp.approval.event;

import com.hwlee.erp.approval.ApprovalDocType;

/**
 * 전자결재가 반려(종결)됐음을 알리는 도메인 이벤트.
 *
 * <p>승인 이벤트와 대칭 — 원본 문서가 자기 상태를 반려로 되돌릴 수 있게 한다(예: 여신 요청 REJECTED).
 * 반려는 문서 상태를 되돌릴 뿐 부수효과(한도 반영 등)는 없다.
 *
 * @param docType        원본 문서 종류
 * @param refId          원본 문서 id
 * @param refNo          원본 문서 번호
 * @param approvalNumber 결재 문서 번호
 * @param decidedBy      반려한 결재자(username)
 * @param reason         반려 사유(결재자 의견)
 */
public record ApprovalRejectedEvent(ApprovalDocType docType, Long refId, String refNo,
                                    String approvalNumber, String decidedBy, String reason) {
}
