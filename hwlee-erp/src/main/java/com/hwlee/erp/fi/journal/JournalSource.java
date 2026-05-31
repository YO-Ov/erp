package com.hwlee.erp.fi.journal;

/**
 * 전표 출처 분류 — 어떤 트랜잭션이 이 전표를 만들었는가.
 *
 * <p>{@link JournalEntry#getSourceType()} + {@link JournalEntry#getSourceId()} 가
 * 다형성 참조(weak reference)를 구성한다. FK 가 아닌 이유:
 * 출처 테이블이 여럿(invoice/goods_issue/goods_receipt/payment/payroll_run + MANUAL) 이라 단일 FK 로 표현 불가.
 * StockMovement.refType/refId 와 같은 패턴.
 */
public enum JournalSource {
    INV,     // Invoice — 매출 전표
    GI,      // GoodsIssue (출하 연계) — 매출원가 전표
    GR,      // GoodsReceipt — 매입 전표
    PAY,     // Payment — 입금/출금 전표
    PAYROLL, // PayrollRun — 급여 확정(인건비)·급여 지급 전표 (Phase 7)
    MANUAL   // 사람이 직접 입력한 수동 전표
}
