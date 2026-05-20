package com.hwlee.erp.common.entity;

/**
 * 마스터 데이터의 활성 상태.
 * Soft Delete(deletedAt)와는 별개로, "지워지진 않았지만 거래 가능 여부" 를 표현한다.
 */
public enum MasterStatus {
    /** 정상 거래 가능 */
    ACTIVE,
    /** 휴면 — 조회는 되지만 새 거래 등록은 막는다 */
    INACTIVE,
    /** 거래 정지 (신용 불량, 계약 종료 등) */
    BLOCKED
}
