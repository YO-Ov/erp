package com.hwlee.erp.approval.event;

import com.hwlee.erp.approval.ApprovalDocType;

/**
 * 전자결재가 최종 승인됐음을 알리는 도메인 이벤트.
 *
 * <p>결재 엔진은 원본 문서(견적 등)를 직접 알지 못한다. 대신 이 이벤트를 발행하고,
 * 각 문서 모듈이 리스너로 받아 자기 문서를 전이시킨다(예: 견적 → SENT). 결재→문서의
 * 역방향 의존을 이벤트로 끊는다.
 *
 * @param docType         원본 문서 종류
 * @param refId           원본 문서 id
 * @param refNo           원본 문서 번호(표시용)
 * @param approvalNumber  결재 문서 번호
 * @param decidedBy       최종 승인한 결재자(username)
 */
public record ApprovalApprovedEvent(ApprovalDocType docType, Long refId, String refNo,
                                    String approvalNumber, String decidedBy) {
}
