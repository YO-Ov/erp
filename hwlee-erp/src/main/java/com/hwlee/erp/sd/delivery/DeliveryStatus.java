package com.hwlee.erp.sd.delivery;

/**
 * 출하 헤더 상태.
 *
 * <pre>
 * DRAFT → SHIPPED → CANCELLED
 * </pre>
 */
public enum DeliveryStatus {
    DRAFT,
    SHIPPED,
    CANCELLED
}
