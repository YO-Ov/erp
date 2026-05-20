package com.hwlee.erp.master.customer;

/**
 * 결제 조건. 고객/거래처가 사용한다.
 *
 * <p>표준 ERP 약식 표기를 따른다:
 * <ul>
 *   <li>{@code NET30} — 거래일로부터 30일 안에 결제
 *   <li>{@code NET60} — 60일
 *   <li>{@code COD}   — Cash On Delivery (현금 결제)
 *   <li>{@code PREPAID} — 선결제
 * </ul>
 */
public enum PaymentTerms {
    NET30,
    NET60,
    COD,
    PREPAID
}
