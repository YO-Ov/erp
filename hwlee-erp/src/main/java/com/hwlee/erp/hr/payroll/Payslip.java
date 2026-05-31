package com.hwlee.erp.hr.payroll;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.employee.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 급여명세서 (라인) — 한 직원의 한 달 급여 내역. {@link PayrollRun} 의 자식.
 *
 * <pre>
 *   gross           = base_pay + overtime_pay
 *   total_deduction = income_tax + insurance_employee   (직원에게서 떼는 것만)
 *   net             = gross - total_deduction           (실수령)
 * </pre>
 *
 * <p>{@code insurance_company} 는 공제가 아니다 — 회사가 별도 부담하는 4대보험(법정복리비).
 * 직원 월급에서 빠지지 않으므로 total_deduction/net 계산엔 들어가지 않고, 회계 전표에서만 쓰인다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payslip")
public class Payslip extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "base_pay", nullable = false, precision = 15, scale = 2)
    private BigDecimal basePay;

    @Column(name = "overtime_pay", nullable = false, precision = 15, scale = 2)
    private BigDecimal overtimePay;

    @Column(name = "gross_pay", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossPay;

    @Column(name = "income_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal incomeTax;

    @Column(name = "insurance_employee", nullable = false, precision = 15, scale = 2)
    private BigDecimal insuranceEmployee;

    @Column(name = "insurance_company", nullable = false, precision = 15, scale = 2)
    private BigDecimal insuranceCompany;

    @Column(name = "total_deduction", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDeduction;

    @Column(name = "net_pay", nullable = false, precision = 15, scale = 2)
    private BigDecimal netPay;

    Payslip(PayrollRun run, Employee employee, BigDecimal basePay, BigDecimal overtimePay,
            BigDecimal incomeTax, BigDecimal insuranceEmployee, BigDecimal insuranceCompany) {
        if (run == null) throw new IllegalArgumentException("payrollRun 은 null 일 수 없다.");
        if (employee == null) throw new IllegalArgumentException("employee 는 null 일 수 없다.");
        this.payrollRun = run;
        this.employee = employee;
        this.basePay = basePay;
        this.overtimePay = overtimePay;
        this.grossPay = basePay.add(overtimePay);
        this.incomeTax = incomeTax;
        this.insuranceEmployee = insuranceEmployee;
        this.insuranceCompany = insuranceCompany;
        this.totalDeduction = incomeTax.add(insuranceEmployee);
        this.netPay = this.grossPay.subtract(this.totalDeduction);
    }
}
