package com.hwlee.erp.hr;

import static org.assertj.core.api.Assertions.assertThat;

import com.hwlee.erp.hr.payroll.PayrollPolicy;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 급여 계산 정책의 순수 단위 테스트 — 세율/연장수당 배율이 의도대로 적용되는지.
 * (Spring/DB 없이 빠르게 도는 계산 검증.)
 */
class PayrollPolicyTest {

    @Test
    @DisplayName("연장수당 = 시급 × 1.5 × (연장 분/60)")
    void 연장수당_계산() {
        // 시급 20,000, 연장 600분(10h) → 20,000 × 1.5 × 10 = 300,000
        assertThat(PayrollPolicy.overtimePay(new BigDecimal("20000"), 600))
                .isEqualByComparingTo("300000");
    }

    @Test
    @DisplayName("연장근로 0분이면 연장수당 0")
    void 연장_없으면_수당_0() {
        assertThat(PayrollPolicy.overtimePay(new BigDecimal("20000"), 0))
                .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("공제·법정복리비 요율 — 소득세 6%, 4대보험 직원/회사 각 4.5%")
    void 공제_요율() {
        BigDecimal gross = new BigDecimal("3000000");
        assertThat(PayrollPolicy.incomeTax(gross)).isEqualByComparingTo("180000");          // 6%
        assertThat(PayrollPolicy.insuranceEmployee(gross)).isEqualByComparingTo("135000");  // 4.5%
        assertThat(PayrollPolicy.insuranceCompany(gross)).isEqualByComparingTo("135000");   // 4.5%
    }
}
