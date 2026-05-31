package com.hwlee.erp.hr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.fi.journal.JournalEntry;
import com.hwlee.erp.fi.journal.JournalEntryRepository;
import com.hwlee.erp.fi.journal.JournalEntryStatus;
import com.hwlee.erp.fi.journal.JournalSource;
import com.hwlee.erp.fi.journal.SystemAccounts;
import com.hwlee.erp.hr.attendance.AttendanceService;
import com.hwlee.erp.hr.attendance.dto.AttendanceCreateRequest;
import com.hwlee.erp.hr.contract.Position;
import com.hwlee.erp.hr.contract.EmploymentContractService;
import com.hwlee.erp.hr.contract.dto.EmploymentContractCreateRequest;
import com.hwlee.erp.hr.payroll.PayrollService;
import com.hwlee.erp.hr.payroll.PayrollStatus;
import com.hwlee.erp.hr.payroll.dto.PayrollRunResponse;
import com.hwlee.erp.hr.payroll.dto.PayslipResponse;
import com.hwlee.erp.master.employee.EmployeeService;
import com.hwlee.erp.master.employee.dto.EmployeeCreateRequest;
import com.hwlee.erp.master.employee.dto.EmployeeResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Phase 7 의 하이라이트 — 근태→급여 계산→확정 이벤트→인건비 전표(복식부기)→지급 전표.
 *
 * <p>핵심 검증:
 * <ol>
 *   <li>근태(연장근로) 기반 급여 계산 — 기본급 + 연장수당(1.5배), 공제(소득세·4대보험), 실수령</li>
 *   <li>급여 확정 시 인건비 전표 자동 생성 + <b>차변 합 = 대변 합</b></li>
 *   <li>회사부담 4대보험 = 법정복리비(비용), 직원 월급 공제 아님</li>
 *   <li>지급(2단계) 시 미지급급여 → 현금 전표</li>
 *   <li>근태 (employee, date) 중복 거부</li>
 * </ol>
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PayrollAccountingIntegrationTest {

    @Autowired EmployeeService employeeService;
    @Autowired EmploymentContractService contractService;
    @Autowired AttendanceService attendanceService;
    @Autowired PayrollService payrollService;
    @Autowired JournalEntryRepository journalRepository;

    @Test
    @DisplayName("근태 기반 급여 계산 → 확정 시 인건비 전표(차변=대변) → 지급 시 미지급급여→현금 전표")
    void 급여_확정_및_지급_자동_분개() {
        // given — 전용 직원 + 급여계약(기본급 3,000,000 / 소정 200h → 시급 15,000)
        EmployeeResponse emp = createEmployee();
        contractService.create(new EmploymentContractCreateRequest(
                emp.id(), Position.STAFF, new BigDecimal("3000000"), 200, LocalDate.of(2026, 7, 1)));

        // 근태: 08:00~18:00 (10h) → 연장 120분(2h). 연장수당 = 15,000 × 1.5 × 2 = 45,000
        attendanceService.create(new AttendanceCreateRequest(
                emp.id(), LocalDate.of(2026, 7, 1), LocalTime.of(8, 0), LocalTime.of(18, 0)));

        // when — 7월 급여대장 계산(DRAFT)
        PayrollRunResponse draft = payrollService.createDraft(
                new com.hwlee.erp.hr.payroll.dto.PayrollRunCreateRequest("2026-07"));
        assertThat(draft.status()).isEqualTo(PayrollStatus.DRAFT);

        // then(1) — 내 직원의 명세 계산 검증
        PayslipResponse slip = draft.payslips().stream()
                .filter(p -> p.employeeId().equals(emp.id()))
                .findFirst().orElseThrow();
        assertThat(slip.basePay()).isEqualByComparingTo("3000000");
        assertThat(slip.overtimePay()).isEqualByComparingTo("45000");
        assertThat(slip.grossPay()).isEqualByComparingTo("3045000");
        assertThat(slip.incomeTax()).isEqualByComparingTo("182700");          // 6%
        assertThat(slip.insuranceEmployee()).isEqualByComparingTo("137025");  // 4.5%
        assertThat(slip.insuranceCompany()).isEqualByComparingTo("137025");   // 4.5% (공제 아님)
        assertThat(slip.totalDeduction()).isEqualByComparingTo("319725");     // 소득세 + 직원분 보험
        assertThat(slip.netPay()).isEqualByComparingTo("2725275");            // gross - 공제

        // when(2) — 확정 → 인건비 전표 자동 생성
        PayrollRunResponse confirmed = payrollService.confirm(draft.id());
        assertThat(confirmed.status()).isEqualTo(PayrollStatus.CONFIRMED);

        // then(2) — 대장 전체 합산 분개. 차변=대변, 계정별 금액이 대장 합과 일치.
        List<JournalEntry> confirmEntries =
                journalRepository.findBySourceTypeAndSourceIdWithLines(JournalSource.PAYROLL, confirmed.id());
        assertThat(confirmEntries).hasSize(1);
        JournalEntry je = confirmEntries.get(0);
        assertThat(je.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(je.getTotalDebit())
                .as("복식부기 불변식 — 차변 합 = 대변 합")
                .isEqualByComparingTo(je.getTotalCredit());

        BigDecimal sumIncomeTax = sumOf(confirmed.payslips(), PayslipResponse::incomeTax);
        BigDecimal sumInsEmployee = sumOf(confirmed.payslips(), PayslipResponse::insuranceEmployee);
        BigDecimal sumInsCompany = sumOf(confirmed.payslips(), PayslipResponse::insuranceCompany);

        assertThat(debitOf(je, SystemAccounts.SALARY_EXPENSE)).isEqualByComparingTo(confirmed.totalGross());
        assertThat(debitOf(je, SystemAccounts.LEGAL_WELFARE)).isEqualByComparingTo(sumInsCompany);
        assertThat(creditOf(je, SystemAccounts.WITHHOLDING_TAX)).isEqualByComparingTo(sumIncomeTax);
        assertThat(creditOf(je, SystemAccounts.WITHHOLDING_INSURANCE))
                .as("예수금-사회보험 = 직원분 + 회사분")
                .isEqualByComparingTo(sumInsEmployee.add(sumInsCompany));
        assertThat(creditOf(je, SystemAccounts.SALARY_PAYABLE)).isEqualByComparingTo(confirmed.totalNet());

        // when(3) — 지급 → 미지급급여 → 현금 전표
        PayrollRunResponse paid = payrollService.markPaid(confirmed.id());
        assertThat(paid.status()).isEqualTo(PayrollStatus.PAID);

        // then(3) — PAYROLL 출처 전표 2건(확정+지급), 지급 전표는 차)미지급급여 / 대)현금
        List<JournalEntry> all =
                journalRepository.findBySourceTypeAndSourceIdWithLines(JournalSource.PAYROLL, paid.id());
        assertThat(all).hasSize(2);
        JournalEntry payEntry = all.stream()
                .filter(e -> e.getDescription().contains("급여지급"))
                .findFirst().orElseThrow();
        assertThat(debitOf(payEntry, SystemAccounts.SALARY_PAYABLE)).isEqualByComparingTo(paid.totalNet());
        assertThat(creditOf(payEntry, SystemAccounts.CASH)).isEqualByComparingTo(paid.totalNet());
    }

    @Test
    @DisplayName("같은 직원·같은 날짜 근태 중복 등록은 거부된다")
    void 근태_중복_거부() {
        EmployeeResponse emp = createEmployee();
        attendanceService.create(new AttendanceCreateRequest(
                emp.id(), LocalDate.of(2026, 7, 10), LocalTime.of(9, 0), LocalTime.of(18, 0)));

        assertThatThrownBy(() -> attendanceService.create(new AttendanceCreateRequest(
                emp.id(), LocalDate.of(2026, 7, 10), LocalTime.of(9, 0), LocalTime.of(18, 0))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // === 헬퍼 ===

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime());

    private EmployeeResponse createEmployee() {
        long n = SEQ.incrementAndGet();
        return employeeService.create(new EmployeeCreateRequest(
                "근로자-" + n, "worker" + n + "@hwlee-erp.example", "DEPT-HR", LocalDate.of(2025, 1, 1)));
    }

    private static BigDecimal sumOf(List<PayslipResponse> slips,
                                    java.util.function.Function<PayslipResponse, BigDecimal> field) {
        return slips.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal debitOf(JournalEntry je, String accountCode) {
        return je.getLines().stream()
                .filter(l -> l.getAccount().getCode().equals(accountCode))
                .map(l -> l.getDebit())
                .findFirst().orElse(BigDecimal.ZERO);
    }

    private static BigDecimal creditOf(JournalEntry je, String accountCode) {
        return je.getLines().stream()
                .filter(l -> l.getAccount().getCode().equals(accountCode))
                .map(l -> l.getCredit())
                .findFirst().orElse(BigDecimal.ZERO);
    }
}
