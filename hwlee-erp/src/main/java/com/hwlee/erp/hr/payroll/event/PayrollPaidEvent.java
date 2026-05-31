package com.hwlee.erp.hr.payroll.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 급여 지급 사건 — {@link com.hwlee.erp.hr.payroll.PayrollService#markPaid} 가 발행 (Phase 7).
 *
 * <p>확정(비용 인식)과 지급(현금 유출)을 분리한 발생주의 2단계 중 두 번째.
 * 회계(FI)가 구독해 지급 전표를 자동 생성한다:
 * <pre>
 *   차) 미지급급여 {totalNet} / 대) 현금 {totalNet}
 * </pre>
 */
public record PayrollPaidEvent(
        Long payrollRunId,
        String number,
        LocalDate paymentDate,
        BigDecimal totalNet
) {}
