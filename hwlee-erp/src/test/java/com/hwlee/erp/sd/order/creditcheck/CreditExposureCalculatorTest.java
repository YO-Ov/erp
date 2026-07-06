package com.hwlee.erp.sd.order.creditcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.hwlee.erp.fi.payment.PaymentRepository;
import com.hwlee.erp.sd.invoice.InvoiceRepository;
import com.hwlee.erp.sd.order.SalesOrderRepository;
import com.hwlee.erp.sd.order.creditcheck.CreditExposureCalculator.CreditExposure;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 여신사용액 산식 단위 검증 — 여신사용액 = ① 미청구 활성수주 + ② 미수금(발행 인보이스 − 입금, 하한 0).
 * DB 없이 세 repository 를 stub 해 "입금 시 여신 자동 해제" 산술을 직접 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class CreditExposureCalculatorTest {

    private static final Long CUST = 1L;

    @Mock SalesOrderRepository salesOrderRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock PaymentRepository paymentRepository;

    private CreditExposure compute(String backlog, String issuedInvoice, String receivedPayment) {
        when(salesOrderRepository.sumUninvoicedActiveOrderAmount(eq(CUST), any()))
                .thenReturn(new BigDecimal(backlog));
        when(invoiceRepository.sumIssuedInvoiceTotalByCustomer(CUST))
                .thenReturn(new BigDecimal(issuedInvoice));
        when(paymentRepository.sumPostedReceiptAmountByCustomer(CUST))
                .thenReturn(new BigDecimal(receivedPayment));
        return new CreditExposureCalculator(salesOrderRepository, invoiceRepository, paymentRepository)
                .compute(CUST, null);
    }

    @Test
    @DisplayName("확정 미청구 수주는 백로그로 전액 잡힌다 (인보이스·입금 없음)")
    void 미청구_수주는_백로그로_전액() {
        CreditExposure e = compute("300000", "0", "0");
        assertThat(e.orderBacklog()).isEqualByComparingTo("300000");
        assertThat(e.receivable()).isEqualByComparingTo("0");
        assertThat(e.used()).isEqualByComparingTo("300000");
    }

    @Test
    @DisplayName("청구되면 백로그→미수금으로 이관 — 미입금이면 여신사용액 유지")
    void 청구_후_미입금이면_미수금으로_유지() {
        // 백로그 0(이미 INVOICED 라 미청구 합에서 빠짐), 인보이스 33만(부가세 포함) 발행, 입금 0
        CreditExposure e = compute("0", "330000", "0");
        assertThat(e.orderBacklog()).isEqualByComparingTo("0");
        assertThat(e.receivable()).isEqualByComparingTo("330000");
        assertThat(e.used()).isEqualByComparingTo("330000");
    }

    @Test
    @DisplayName("입금이 들어오면 미수금이 줄어 여신사용액이 자동 해제된다")
    void 입금하면_여신_자동_해제() {
        // 33만 인보이스 중 20만 입금 → 미수금 13만만 남음
        CreditExposure partial = compute("0", "330000", "200000");
        assertThat(partial.receivable()).isEqualByComparingTo("130000");
        assertThat(partial.used()).isEqualByComparingTo("130000");

        // 전액(33만) 입금 → 미수금 0 → 여신사용액 0 (완전 해제)
        CreditExposure full = compute("0", "330000", "330000");
        assertThat(full.receivable()).isEqualByComparingTo("0");
        assertThat(full.used()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("과거 청산 거래(입금 ≥ 인보이스)는 미수금이 음수로 새지 않고 0 으로 막힌다")
    void 과잉_입금은_음수가_아니라_0() {
        // 3년치 시드처럼 입금이 인보이스보다 많아도 미수금은 0 이 하한
        CreditExposure e = compute("0", "1000000", "1500000");
        assertThat(e.receivable()).isEqualByComparingTo("0");
        assertThat(e.used()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("미청구 백로그와 미수금이 함께 있으면 합산된다")
    void 백로그와_미수금_합산() {
        // 신규 확정 50만(미청구) + 과거 청구분 미수 13만
        CreditExposure e = compute("500000", "330000", "200000");
        assertThat(e.orderBacklog()).isEqualByComparingTo("500000");
        assertThat(e.receivable()).isEqualByComparingTo("130000");
        assertThat(e.used()).isEqualByComparingTo("630000");
    }
}
