package com.hwlee.erp.approval;

/**
 * 결재 단계 유형 — 한국 실무 전자결재의 세 갈래.
 *
 * <ul>
 *   <li>{@link #APPROVAL} 결재(승인): 순차 처리. 앞 단계가 승인돼야 다음 차례. 반려·반송 권한 있음.
 *   <li>{@link #AGREEMENT} 합의(협조): 관련 부서의 동의. 순차와 무관하게 병렬로 처리 가능.
 *   <li>{@link #REFERENCE} 참조(열람): 결재권 없이 통보만 받음. 처리 대상이 아니다.
 * </ul>
 */
public enum ApprovalStepType {
    APPROVAL("결재"),
    AGREEMENT("합의"),
    REFERENCE("참조");

    private final String label;

    ApprovalStepType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
