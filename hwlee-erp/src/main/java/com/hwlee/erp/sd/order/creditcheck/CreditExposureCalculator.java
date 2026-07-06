package com.hwlee.erp.sd.order.creditcheck;

import com.hwlee.erp.fi.payment.PaymentRepository;
import com.hwlee.erp.sd.invoice.InvoiceRepository;
import com.hwlee.erp.sd.order.SalesOrderRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 한 고객의 "여신사용액(사용 중인 신용)"을 계산한다 — 검증({@link CreditLimitChecker})과
 * 화면 조회가 공유하는 단일 산식.
 *
 * <p><b>여신사용액 = ① 미청구 활성수주 + ② 미수금(AR)</b>
 * <ul>
 *   <li>① <b>미청구 활성수주</b>: 확정됐지만 아직 인보이스 발행 전인 수주
 *       (CONFIRMED·SHIPPING·SHIPPED)의 합. 아직 매출채권으로 안 넘어간 익스포저.</li>
 *   <li>② <b>미수금(AR)</b>: 발행(ISSUED)된 인보이스 합계 − 확정(POSTED)된 입금 합계(하한 0).
 *       인보이스가 나가면 ①에서 빠지는 대신 여기로 이관되고, <b>입금이 들어오면 그만큼 줄어든다</b>
 *       → 이것이 "입금 시 여신 자동 해제"의 핵심.</li>
 * </ul>
 *
 * <p>과거(CLOSED) 거래는 인보이스와 입금이 서로 상쇄돼 ②에 0으로 기여하므로, 3년치 시드가
 * 쌓여 있어도 현재 익스포저만 정확히 잡힌다. (전량 입금 없이 CLOSED 된 수주의 미수는 ②에 잔존 = 정상.)
 *
 * <p>입금(Payment)은 특정 인보이스에 매칭되지 않으므로 미수금은 고객 단위 합계 차감으로 파생한다
 * (전용 Receivable 엔티티 없음 — {@code batch/closing/ArAgingBuckets} 와 동일 사상).
 *
 * <p><b>단순화</b>: 부분청구(INVOICING) 수주는 ①에서 제외하므로, 아직 인보이스가 안 나간
 * 잔량은 잠시 여신사용액에서 빠진다. 인보이스는 발행과 SO 라인 누적이 한 트랜잭션이라
 * (INVOICED = 전량 ISSUED 인보이스 존재) 이 갭은 부분청구 진행 중에만 생기는 과소집계다.
 */
@Component
@RequiredArgsConstructor
public class CreditExposureCalculator {

    private final SalesOrderRepository salesOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    /**
     * @param customerId    대상 고객
     * @param excludeOrderId 미청구 백로그에서 제외할 수주 id(재확인·수정 시 자기 자신 제외). null 이면 없음.
     */
    public CreditExposure compute(Long customerId, Long excludeOrderId) {
        BigDecimal backlog = nz(salesOrderRepository
                .sumUninvoicedActiveOrderAmount(customerId, excludeOrderId));
        BigDecimal invoiced = nz(invoiceRepository.sumIssuedInvoiceTotalByCustomer(customerId));
        BigDecimal received = nz(paymentRepository.sumPostedReceiptAmountByCustomer(customerId));
        BigDecimal receivable = invoiced.subtract(received).max(BigDecimal.ZERO);
        return new CreditExposure(backlog, receivable);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 여신사용액 분해값. {@code used() = orderBacklog + receivable}. */
    public record CreditExposure(BigDecimal orderBacklog, BigDecimal receivable) {
        public BigDecimal used() {
            return orderBacklog.add(receivable);
        }
    }
}
