package com.hwlee.erp.sd.order;

/**
 * 수주(SalesOrder) 헤더 상태.
 *
 * <pre>
 * DRAFT → CONFIRMED → SHIPPING ↔ SHIPPED → INVOICING ↔ INVOICED → CLOSED
 *       ↘ CANCELLED (DRAFT/CONFIRMED 에서만)
 * </pre>
 */
public enum SalesOrderStatus {
    DRAFT,
    CONFIRMED,
    SHIPPING,
    SHIPPED,
    INVOICING,
    INVOICED,
    CLOSED,
    CANCELLED
}
