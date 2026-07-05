package com.hwlee.erp.approval;

/**
 * 결재선 각 단계의 처리 상태.
 *
 * <ul>
 *   <li>{@link #PENDING} 미처리 — 아직 결재/합의 전.
 *   <li>{@link #APPROVED} 처리됨 — 결재는 승인, 합의는 동의.
 *   <li>{@link #REJECTED} 반려 — 이 단계에서 문서 전체가 반려됨.
 *   <li>{@link #SKIPPED} 건너뜀 — (예약) 전결로 상위 단계가 생략된 경우.
 * </ul>
 *
 * <p>참조(REFERENCE) 단계는 처리 대상이 아니므로 계속 PENDING(=미열람) 으로 둔다.
 */
public enum ApprovalStepStatus {
    PENDING,
    APPROVED,
    REJECTED,
    SKIPPED
}
