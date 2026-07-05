package com.hwlee.erp.approval;

/**
 * 전결(前決) 레벨 — 금액 구간에 따라 "어느 직급까지 결재가 올라가는가".
 *
 * <p>실무의 전결권한(delegation of authority) = 전결 규정. 소액은 팀장 선에서 끝나고(전결),
 * 고액일수록 본부장·대표까지 올라간다. 상신자 부서에서 조직 트리를 몇 단계까지 타고
 * 올라갈지를 이 값이 결정한다.
 *
 * <ul>
 *   <li>{@link #TEAM} 팀장 전결 — 상신자 부서(팀)의 부서장 1명.
 *   <li>{@link #DIVISION} 본부장까지 — 팀장 + 상위 본부장.
 *   <li>{@link #COMPANY} 대표까지 — 팀장 + 본부장 + 회사 대표.
 * </ul>
 */
public enum ApprovalLevel {
    TEAM(1),
    DIVISION(2),
    COMPANY(Integer.MAX_VALUE);

    /**
     * 확보할 결재자(승인 단계) 수. 상신자 부서에서 위로 트리를 훑으며 부서장을 모으되,
     * 자기 자신·부서장 미지정 노드는 건너뛰고 상위로 밀린다. 이 수만큼 모이면 멈춘다.
     * (상신자가 팀장이면 자기 팀 단계는 생략되고 본부장이 첫 결재자가 된다.)
     */
    private final int targetApprovers;

    ApprovalLevel(int targetApprovers) {
        this.targetApprovers = targetApprovers;
    }

    public int targetApprovers() {
        return targetApprovers;
    }
}
