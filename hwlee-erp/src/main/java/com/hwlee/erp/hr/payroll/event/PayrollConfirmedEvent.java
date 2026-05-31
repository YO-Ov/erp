package com.hwlee.erp.hr.payroll.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 급여 확정 사건 — {@link com.hwlee.erp.hr.payroll.PayrollService#confirm} 가 발행 (Phase 7).
 *
 * <p>회계(FI) 모듈이 구독해 인건비 전표를 자동 생성한다:
 * <pre>
 *   차) 급여비용   {totalGross}
 *   차) 법정복리비 {totalInsuranceCompany}
 *           대) 예수금-소득세   {totalIncomeTax}
 *           대) 예수금-사회보험 {totalInsuranceEmployee + totalInsuranceCompany}
 *           대) 미지급급여      {totalNet}
 * </pre>
 *
 * <p>{@link com.hwlee.erp.sd.invoice.event.InvoiceIssuedEvent} 와 같은 패턴 — 이벤트는 발행자(HR)가 소유,
 * 리스너({@code fi/integration/hr/})는 수신자(FI)가 소유. HR 은 FI 를 직접 import 하지 않는다.
 */
public record PayrollConfirmedEvent(
        Long payrollRunId,
        String number,
        LocalDate entryDate,
        BigDecimal totalGross,
        BigDecimal totalIncomeTax,
        BigDecimal totalInsuranceEmployee,
        BigDecimal totalInsuranceCompany,
        BigDecimal totalNet
) {}
