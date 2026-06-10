package com.hwlee.erp.fi.credit;

/**
 * 여신(신용한도) 상향 요청 상태.
 *
 * <pre>
 * PENDING ──승인(approve)──▶ APPROVED  (고객 한도 상향 반영)
 *    └──────거부(reject)────▶ REJECTED
 * </pre>
 */
public enum CreditLimitRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
