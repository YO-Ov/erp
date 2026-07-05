package com.hwlee.erp.approval;

/**
 * 결재자가 한 단계에서 취할 수 있는 행동.
 *
 * <ul>
 *   <li>{@link #APPROVE} 승인/합의 — 다음 단계로 진행(승인) 또는 동의(합의).
 *   <li>{@link #REJECT} 반려 — 문서 전체를 종결(REJECTED). 상신자는 새 문서로 다시 올려야 한다.
 *   <li>{@link #RETURN} 반송 — 상신자에게 되돌림(DRAFT). 수정 후 재상신 가능. 결재(APPROVAL) 단계만 가능.
 * </ul>
 */
public enum ApprovalAction {
    APPROVE,
    REJECT,
    RETURN
}
