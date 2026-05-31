package com.hwlee.erp.hr.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 급여 계산 정책 — 세율/수당 배율을 한곳에 모은 상수 + 계산 헬퍼.
 *
 * <p>학습용 고정값이다. 실제 4대보험은 항목별(국민연금·건강·고용·산재) 요율이 다르고 상한이 있지만,
 * "근태→급여→공제→실수령" 구조를 보이는 게 목적이라 단순화했다. 값이 한눈에 보이도록 상수로 분리.
 *
 * <p>금액은 모두 원 단위(반올림)로 떨어뜨린다.
 */
public final class PayrollPolicy {

    /** 연장근로 가산 배율 — 통상임금의 1.5배(근로기준법 가산율 50%). */
    public static final BigDecimal OVERTIME_MULTIPLIER = new BigDecimal("1.5");

    /** 소득세 간이 요율 6%. */
    public static final BigDecimal INCOME_TAX_RATE = new BigDecimal("0.06");

    /** 4대보험 직원 부담 요율 4.5% (월급에서 공제). */
    public static final BigDecimal INSURANCE_EMPLOYEE_RATE = new BigDecimal("0.045");

    /** 4대보험 회사 부담 요율 4.5% (공제 아님 — 회사가 추가로 부담 = 법정복리비). */
    public static final BigDecimal INSURANCE_COMPANY_RATE = new BigDecimal("0.045");

    private static final BigDecimal MINUTES_PER_HOUR = new BigDecimal("60");

    private PayrollPolicy() {}

    /** 연장수당 = 시급 × 1.5 × (연장근로 분 / 60). */
    public static BigDecimal overtimePay(BigDecimal hourlyWage, int overtimeMinutes) {
        if (overtimeMinutes <= 0) {
            return BigDecimal.ZERO.setScale(0);
        }
        return hourlyWage
                .multiply(OVERTIME_MULTIPLIER)
                .multiply(BigDecimal.valueOf(overtimeMinutes))
                .divide(MINUTES_PER_HOUR, 0, RoundingMode.HALF_UP);
    }

    /** 소득세 공제액 = gross × 6%. */
    public static BigDecimal incomeTax(BigDecimal grossPay) {
        return grossPay.multiply(INCOME_TAX_RATE).setScale(0, RoundingMode.HALF_UP);
    }

    /** 4대보험 직원분 공제액 = gross × 4.5%. */
    public static BigDecimal insuranceEmployee(BigDecimal grossPay) {
        return grossPay.multiply(INSURANCE_EMPLOYEE_RATE).setScale(0, RoundingMode.HALF_UP);
    }

    /** 4대보험 회사분(법정복리비) = gross × 4.5%. */
    public static BigDecimal insuranceCompany(BigDecimal grossPay) {
        return grossPay.multiply(INSURANCE_COMPANY_RATE).setScale(0, RoundingMode.HALF_UP);
    }
}
