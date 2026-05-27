package com.hwlee.erp.mm.goodsreceipt;

/**
 * 입고 헤더 상태.
 *
 * <pre>
 * DRAFT → POSTED → CANCELLED
 * </pre>
 */
public enum GoodsReceiptStatus {
    DRAFT,
    POSTED,
    CANCELLED
}
