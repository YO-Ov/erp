package com.hwlee.erp.hr.contract;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.employee.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 급여계약 — 한 직원의 "발효일 기준 급여 조건" 이력.
 *
 * <p>왜 Employee 와 분리한 이력 테이블인가: 연봉은 매년 바뀐다. "2025년 3,000만 → 2026년 3,300만"을
 * {@code effectiveFrom/effectiveTo} 로 표현하고, 급여 계산 시 "그 달에 유효한 계약"을 골라 쓴다.
 *
 * <p>시급 = {@code baseSalary / contractedHours} — 연장수당 계산의 분모.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "employment_contract")
public class EmploymentContract extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false, length = 16)
    private Position position;

    @Column(name = "base_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "contracted_hours", nullable = false)
    private int contractedHours;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ContractStatus status = ContractStatus.ACTIVE;

    public static EmploymentContract create(Employee employee, Position position, BigDecimal baseSalary,
                                            int contractedHours, LocalDate effectiveFrom) {
        if (employee == null) throw new IllegalArgumentException("employee 는 null 일 수 없다.");
        if (position == null) throw new IllegalArgumentException("position 은 null 일 수 없다.");
        if (baseSalary == null || baseSalary.signum() <= 0)
            throw new IllegalArgumentException("baseSalary 는 > 0 이어야 한다.");
        if (contractedHours <= 0)
            throw new IllegalArgumentException("contractedHours 는 > 0 이어야 한다.");
        if (effectiveFrom == null) throw new IllegalArgumentException("effectiveFrom 은 null 일 수 없다.");
        EmploymentContract c = new EmploymentContract();
        c.employee = employee;
        c.position = position;
        c.baseSalary = baseSalary;
        c.contractedHours = contractedHours;
        c.effectiveFrom = effectiveFrom;
        return c;
    }

    /**
     * 계약 종료 — effectiveTo 설정 후 INACTIVE. 새 계약 발효 직전에 직전 계약을 닫을 때 쓴다.
     */
    public void terminate(LocalDate effectiveTo) {
        if (status != ContractStatus.ACTIVE)
            throw new IllegalStateException("ACTIVE 계약만 종료 가능합니다. 현재: " + status);
        if (effectiveTo == null) throw new IllegalArgumentException("effectiveTo 는 null 일 수 없다.");
        if (effectiveTo.isBefore(effectiveFrom))
            throw new IllegalArgumentException("effectiveTo 는 effectiveFrom 이후여야 한다.");
        this.effectiveTo = effectiveTo;
        this.status = ContractStatus.INACTIVE;
    }

    /** 시급 = 월 기본급 / 월 소정근로시간 (원 단위, 반올림). */
    public BigDecimal hourlyWage() {
        return baseSalary.divide(BigDecimal.valueOf(contractedHours), 2, RoundingMode.HALF_UP);
    }

    /** 주어진 일자에 이 계약이 유효한가 (발효일 ≤ date ≤ 만료일, 만료일 null 이면 현재 유효). */
    public boolean isEffectiveOn(LocalDate date) {
        if (date.isBefore(effectiveFrom)) return false;
        return effectiveTo == null || !date.isAfter(effectiveTo);
    }
}
