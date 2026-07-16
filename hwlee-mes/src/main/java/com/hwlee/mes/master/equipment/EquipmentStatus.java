package com.hwlee.mes.master.equipment;

/** 설비 가동 상태. */
public enum EquipmentStatus {
    RUNNING,      // 가동 — 실제로 돌아간 시간(가동시간). 시간가동률의 분자.
    IDLE,         // 대기 — 계획정지(주문 없음/대기)로 보아 부하시간에서 제외.
    DOWN,         // 고장/정지 — 비계획정지. 부하시간에 포함되어 가동률을 떨어뜨린다.
    MAINTENANCE;  // 정비 — 계획보전 = 계획정지로 보아 부하시간에서 제외.

    /** 실제 가동시간(Operating)으로 집계되는 상태 = 시간가동률의 분자. */
    public boolean isOperating() {
        return this == RUNNING;
    }

    /**
     * 부하시간(Loading = 돌리기로 계획된 시간)에 포함되는 상태 = 시간가동률의 분모.
     *
     * <p>가동(RUNNING)과 비계획정지(DOWN·고장)만 부하시간에 넣는다. 대기(IDLE)·정비(MAINTENANCE)는
     * 계획정지로 보아 분모에서 제외 → 놀리거나 계획보전하는 시간은 가동률을 떨어뜨리지 않는다.
     * (실무 OEE 의 "부하시간 = 조업시간 − 계획정지" 정의를 단순화한 것.)
     */
    public boolean countsAsLoading() {
        return this == RUNNING || this == DOWN;
    }
}
