package com.hwlee.erp.sd.invoice.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 인보이스 발행 사건 — {@link com.hwlee.erp.sd.invoice.InvoiceService#create} 가 발행.
 *
 * <p>Phase 5 신규. 회계(FI) 모듈이 구독해 매출 분개를 자동 생성한다:
 * <pre>
 *   차) 매출채권 {totalAmount} / 대) 매출 {subtotal} + 부가세예수금 {taxAmount}
 * </pre>
 *
 * <p>{@link com.hwlee.erp.sd.delivery.event.DeliveryShippedEvent} 와 같은 패턴 — 이벤트는 발행자가 소유.
 */
public record InvoiceIssuedEvent(
        Long invoiceId,
        String number,
        LocalDate invoiceDate,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal totalAmount
) {}
