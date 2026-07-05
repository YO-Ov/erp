package com.hwlee.erp.approval;

/**
 * 결재 문서 전체 상태.
 *
 * <pre>
 * DRAFT ──(상신)──▶ PENDING ──(전 단계 처리 완료)──▶ APPROVED ──▶ [원본 문서 진행]
 *   ▲                  │
 *   │                  ├──(어느 단계든 반려)──▶ REJECTED (종결)
 *   └──(반송)──────────┤
 *                      └──(상신자 회수)──────▶ WITHDRAWN (종결)
 * </pre>
 *
 * <p>반송(RETURN)은 별도 상태가 아니라 DRAFT 로 되돌리는 동작이다 — 상신자가 수정 후 재상신한다.
 */
public enum ApprovalStatus {
    DRAFT,
    PENDING,
    APPROVED,
    REJECTED,
    WITHDRAWN
}
