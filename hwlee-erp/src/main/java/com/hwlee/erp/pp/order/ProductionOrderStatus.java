package com.hwlee.erp.pp.order;

/**
 * 생산지시 상태.
 * <pre>
 * PLANNED ──release()──▶ RELEASED ──complete()──▶ COMPLETED
 *   │ (생성: BOM 전개,    (생산 착수 확정)   (부품 출고 + 완제품 입고,
 *   │  소요자재 산출)                       재고 실제 이동)
 *   └──cancel()──┐    └──cancel()──┐
 *                ▼                 ▼
 *            CANCELLED         CANCELLED
 * </pre>
 * 재고 이동은 COMPLETED 전이에서만 일어난다(완료 이전엔 재고 미반영 → 취소 시 원복 불필요).
 */
public enum ProductionOrderStatus {
    PLANNED,
    RELEASED,
    COMPLETED,
    CANCELLED
}
