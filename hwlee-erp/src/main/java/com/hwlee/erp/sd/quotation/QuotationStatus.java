package com.hwlee.erp.sd.quotation;

/**
 * 견적 헤더 상태.
 *
 * <pre>
 * DRAFT → SENT → ACCEPTED
 *            ↘ EXPIRED  (valid_until 경과)
 *     ↘ CANCELLED
 * </pre>
 */
public enum QuotationStatus {
    DRAFT,
    SENT,
    ACCEPTED,
    EXPIRED,
    CANCELLED
}
