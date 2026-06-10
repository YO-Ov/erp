package com.hwlee.erp.pp.planning;

/**
 * 계획오더 상태 (MRP 제안의 일생).
 *
 * <pre>
 * PROPOSED ──승인(convert)──▶ CONVERTED   (생산지시로 전환됨)
 *    └──────기각(dismiss)────▶ DISMISSED  (담당자가 "생산 안 함" 판단)
 * </pre>
 *
 * <p>계획오더는 "제안" 일 뿐 자재·설비를 잡지 않는다. 실제 생산은 담당자가 {@code convert} 로
 * 생산지시(ProductionOrder)를 만드는 순간 시작된다 — 견적 → 수주 전환과 같은 패턴.
 */
public enum PlannedOrderStatus {
    PROPOSED,
    CONVERTED,
    DISMISSED
}
