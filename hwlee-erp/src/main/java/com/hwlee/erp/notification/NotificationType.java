package com.hwlee.erp.notification;

/**
 * 알림 종류 — 프론트의 아이콘/색 매핑 키이자, 어떤 사건에서 났는지의 분류.
 */
public enum NotificationType {
    /** 생산지시 취소 → 출처 수주의 부족분이 다시 미해결 (영업에게). */
    PRODUCTION_CANCELLED,
    /** 여신(신용한도) 상향 요청 접수 (재무에게). */
    CREDIT_REQUEST_SUBMITTED,
    /** 여신 상향 요청 승인됨 (요청한 영업에게). */
    CREDIT_REQUEST_APPROVED,
    /** 여신 상향 요청 거부됨 (요청한 영업에게). */
    CREDIT_REQUEST_REJECTED
}
