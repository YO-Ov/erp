package com.hwlee.erp.sd.quotation;

/**
 * 견적 헤더 상태.
 *
 * <pre>
 * DRAFT → APPROVED → SENT → ACCEPTED → CONVERTED  (수주로 전환되어 소비됨)
 *                       ↘ EXPIRED  (valid_until 경과)
 *     ↘ CANCELLED
 * </pre>
 *
 * <p>{@code APPROVED}: 전자결재 최종 승인 = "이 조건으로 나가도 좋다"는 내부 재가. 아직 고객 발송 전.
 * 담당자가 확인 후 발송(send)하면 SENT 로 넘어간다(승인과 발송을 분리).
 *
 * <p>{@code CONVERTED}: 이 견적으로 수주를 한 번 생성하면 ACCEPTED → CONVERTED 로 종료된다.
 * 견적당 수주는 1건만 — 같은 견적으로 중복 수주를 막는다(분할 수주는 견적 없이 진행).
 * 그 수주가 취소되면 CONVERTED → ACCEPTED 로 되돌아가 견적을 재사용할 수 있다.
 */
public enum QuotationStatus {
    DRAFT,
    APPROVED,
    SENT,
    ACCEPTED,
    CONVERTED,
    EXPIRED,
    CANCELLED
}
