package com.hwlee.erp.mm.purchaseorder;

/**
 * 구매발주(PO) 상태.
 *
 * <p>흐름: {@code DRAFT →(결재 상신·최종 승인)→ CONFIRMED →(입고 완료)→ CLOSED}.
 * DRAFT/CONFIRMED 에서 취소({@link #CANCELLED}) 가능. 결재 "진행 중"은 PO 자체 상태가 아니라
 * 결재 엔진(approval)의 상태로 관리되며, 목록/상세에서 결재 상태 배지로 표시한다(견적·지급과 동일).
 */
public enum PurchaseOrderStatus {
    /** 작성 중 — 수정 가능, 아직 발주 전. 결재 상신 대상. */
    DRAFT,
    /** 발주 확정 — 전자결재 최종 승인으로 전이. 거래처에 발주된 상태. */
    CONFIRMED,
    /** 종료 — 입고가 완료되어 발주를 마감. terminal. */
    CLOSED,
    /** 취소 — DRAFT/CONFIRMED 에서만 진입. terminal. */
    CANCELLED
}
