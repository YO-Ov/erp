package com.hwlee.erp.mm.goodsissue;

/**
 * 출고 사유. Phase 3 에서는 출고가 일어나는 비즈니스 맥락만 표시 (Customer/SO 참조는 Phase 4).
 */
public enum GoodsIssueReason {
    /** 출하 (Phase 4 에서 Delivery 와 연계 예정) */
    SHIPMENT,
    /** 실사 조정 (음의 방향) */
    ADJUSTMENT,
    /** 폐기/불량 */
    SCRAP
}
