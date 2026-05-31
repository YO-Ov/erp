package com.hwlee.erp.hr.payroll;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.employee.Employee;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 급여대장 (헤더) — "YYYY-MM 한 달치 급여" 묶음. 직원별 명세는 {@link Payslip} 라인.
 *
 * <p>합계 컬럼(total_gross/deduction/net)은 payslip 집계 캐시(조회 편의). period 에 UNIQUE —
 * 한 달에 급여대장 하나.
 *
 * <p>세부 공제 합(소득세/4대보험 직원분/회사분)은 컬럼으로 두지 않고 payslip 에서 그때그때 집계한다
 * — 급여 확정 시 회계 이벤트를 채울 때만 필요해서.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payroll_run")
public class PayrollRun extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30, updatable = false)
    private String number;

    @Column(name = "period", nullable = false, unique = true, length = 7, updatable = false)
    private String period;   // YYYY-MM

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PayrollStatus status = PayrollStatus.DRAFT;

    @Column(name = "total_gross", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalGross = BigDecimal.ZERO;

    @Column(name = "total_deduction", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDeduction = BigDecimal.ZERO;

    @Column(name = "total_net", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalNet = BigDecimal.ZERO;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToMany(mappedBy = "payrollRun", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<Payslip> payslips = new ArrayList<>();

    public static PayrollRun draft(String number, String period, LocalDate runDate) {
        if (number == null || number.isBlank()) throw new IllegalArgumentException("number 는 비어 있을 수 없다.");
        if (period == null || period.isBlank()) throw new IllegalArgumentException("period 는 비어 있을 수 없다.");
        if (runDate == null) throw new IllegalArgumentException("runDate 는 null 일 수 없다.");
        PayrollRun run = new PayrollRun();
        run.number = number;
        run.period = period;
        run.runDate = runDate;
        return run;
    }

    /** 명세서 라인 추가 — DRAFT 에서만. 합계를 즉시 재계산한다. */
    public Payslip addPayslip(Employee employee, BigDecimal basePay, BigDecimal overtimePay,
                              BigDecimal incomeTax, BigDecimal insuranceEmployee, BigDecimal insuranceCompany) {
        if (status != PayrollStatus.DRAFT)
            throw new IllegalStateException("DRAFT 급여대장에만 명세를 추가할 수 있습니다. 현재: " + status);
        Payslip slip = new Payslip(this, employee, basePay, overtimePay,
                incomeTax, insuranceEmployee, insuranceCompany);
        payslips.add(slip);
        recalculateTotals();
        return slip;
    }

    /** DRAFT → CONFIRMED. 급여 계산 확정 시점 — 회계(인건비) 전표가 이 직후 만들어진다. */
    public void confirm(LocalDateTime now) {
        if (status != PayrollStatus.DRAFT)
            throw new IllegalStateException("DRAFT 급여대장만 확정 가능합니다. 현재: " + status);
        if (payslips.isEmpty())
            throw new IllegalStateException("명세가 없는 급여대장은 확정할 수 없습니다.");
        this.status = PayrollStatus.CONFIRMED;
        this.confirmedAt = now;
    }

    /** CONFIRMED → PAID. 실제 이체 시점 — 지급 전표(미지급급여→현금)가 이 직후 만들어진다. */
    public void markPaid(LocalDateTime now) {
        if (status != PayrollStatus.CONFIRMED)
            throw new IllegalStateException("CONFIRMED 급여대장만 지급 처리 가능합니다. 현재: " + status);
        this.status = PayrollStatus.PAID;
        this.paidAt = now;
    }

    public List<Payslip> getPayslips() {
        return Collections.unmodifiableList(payslips);
    }

    /** 소득세 공제 합 — 회계 이벤트(예수금-소득세)용. */
    public BigDecimal totalIncomeTax() {
        return sum(Payslip::getIncomeTax);
    }

    /** 4대보험 직원분 합 — 회계 이벤트(예수금-사회보험)용. */
    public BigDecimal totalInsuranceEmployee() {
        return sum(Payslip::getInsuranceEmployee);
    }

    /** 4대보험 회사분 합 — 회계 이벤트(법정복리비 + 예수금-사회보험)용. */
    public BigDecimal totalInsuranceCompany() {
        return sum(Payslip::getInsuranceCompany);
    }

    private void recalculateTotals() {
        this.totalGross = sum(Payslip::getGrossPay);
        this.totalDeduction = sum(Payslip::getTotalDeduction);
        this.totalNet = sum(Payslip::getNetPay);
    }

    private BigDecimal sum(java.util.function.Function<Payslip, BigDecimal> field) {
        return payslips.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
