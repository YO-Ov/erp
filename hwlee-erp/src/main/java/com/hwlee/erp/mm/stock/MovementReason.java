package com.hwlee.erp.mm.stock;

/**
 * StockMovement 의 발생 사유. 부호 방향이 정해져 있다 — 도메인이 강제한다.
 */
public enum MovementReason {

    /** 매입 입고 — qty_delta > 0 */
    GOODS_RECEIPT(true),
    /** 출고 (출하/조정/폐기 등 다양한 출고를 한 사유로) — qty_delta < 0 */
    GOODS_ISSUE(false),
    /** 실사 조정 — 음/양 둘 다 가능하므로 분리 */
    ADJUSTMENT_PLUS(true),
    ADJUSTMENT_MINUS(false),
    /** 폐기 */
    SCRAP(false),

    /** 생산 투입 — 부품 소모 (Phase 8 PP). qty_delta < 0 */
    PRODUCTION_OUT(false),
    /** 생산 산출 — 완제품 입고 (Phase 8 PP). qty_delta > 0 */
    PRODUCTION_IN(true);

    private final boolean positive;

    MovementReason(boolean positive) {
        this.positive = positive;
    }

    public boolean isPositive() { return positive; }
    public boolean isNegative() { return !positive; }
}
