package com.hwlee.erp.batch.closing;

import com.hwlee.erp.sd.invoice.Invoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 한 고객의 미수금을 경과일 버킷으로 분류한 결과(0~30 / 31~60 / 61~90 / 91+).
 *
 * <p>계산 규칙(학습용 간이 AR Aging):
 * <ul>
 *   <li>입금(RECEIPT)은 특정 인보이스에 매칭되지 않으므로, <b>오래된 인보이스부터 FIFO 로 소진</b>한다.</li>
 *   <li>소진 후 남은 인보이스 잔액을 인보이스일 기준 경과일로 버킷에 배정한다.</li>
 * </ul>
 */
public record ArAgingBuckets(
        BigDecimal bucket0to30,
        BigDecimal bucket31to60,
        BigDecimal bucket61to90,
        BigDecimal bucketOver90,
        BigDecimal total) {

    /**
     * @param agingDate          노령화 기준일
     * @param invoicesOldestFirst 한 고객의 ISSUED 인보이스(인보이스일 오름차순)
     * @param paidTotal          한 고객의 누적 입금액(RECEIPT, POSTED 합)
     */
    public static ArAgingBuckets compute(LocalDate agingDate,
                                         List<Invoice> invoicesOldestFirst,
                                         BigDecimal paidTotal) {
        BigDecimal b0to30 = BigDecimal.ZERO;
        BigDecimal b31to60 = BigDecimal.ZERO;
        BigDecimal b61to90 = BigDecimal.ZERO;
        BigDecimal bOver90 = BigDecimal.ZERO;

        BigDecimal remainingPaid = paidTotal;
        for (Invoice inv : invoicesOldestFirst) {
            BigDecimal open = inv.getTotalAmount();
            // FIFO: 입금액으로 오래된 인보이스부터 차감.
            if (remainingPaid.signum() > 0) {
                BigDecimal applied = remainingPaid.min(open);
                open = open.subtract(applied);
                remainingPaid = remainingPaid.subtract(applied);
            }
            if (open.signum() <= 0) {
                continue; // 전액 회수됨
            }
            long days = ChronoUnit.DAYS.between(inv.getInvoiceDate(), agingDate);
            if (days <= 30) {
                b0to30 = b0to30.add(open);
            } else if (days <= 60) {
                b31to60 = b31to60.add(open);
            } else if (days <= 90) {
                b61to90 = b61to90.add(open);
            } else {
                bOver90 = bOver90.add(open);
            }
        }
        BigDecimal total = b0to30.add(b31to60).add(b61to90).add(bOver90);
        return new ArAgingBuckets(b0to30, b31to60, b61to90, bOver90, total);
    }
}
