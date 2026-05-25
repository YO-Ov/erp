package com.hwlee.erp.sd.invoice;

/**
 * 인보이스 헤더 상태.
 *
 * <pre>
 * DRAFT → ISSUED → CANCELLED
 * </pre>
 */
public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    CANCELLED
}
