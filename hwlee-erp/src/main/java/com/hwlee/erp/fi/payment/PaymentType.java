package com.hwlee.erp.fi.payment;

/**
 * 입금/출금 구분.
 *
 * <ul>
 *   <li>{@link #RECEIPT} 입금 — 고객이 외상값을 갚음. 차)현금 / 대)매출채권. customer 만 채워짐.</li>
 *   <li>{@link #DISBURSEMENT} 출금 — 거래처에 지급. 차)매입채무 / 대)현금. vendor 만 채워짐.</li>
 * </ul>
 */
public enum PaymentType {
    RECEIPT,
    DISBURSEMENT
}
