package com.hwlee.erp.hr.payroll;

/**
 * 급여대장(PayrollRun) 상태.
 * <pre>
 * DRAFT ──confirm()──▶ CONFIRMED ──markPaid()──▶ PAID
 *        (급여 계산 확정          (실제 이체 완료
 *         + 인건비 전표 생성)       + 지급 전표 생성)
 * </pre>
 * 확정=비용 인식, 지급=현금 유출 — 발생주의의 2단계.
 */
public enum PayrollStatus {
    DRAFT,
    CONFIRMED,
    PAID
}
