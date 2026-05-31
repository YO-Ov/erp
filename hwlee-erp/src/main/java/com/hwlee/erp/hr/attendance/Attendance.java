package com.hwlee.erp.hr.attendance;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.employee.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 근태 — 하루 한 건씩 쌓이는 이력 (Phase 3 StockMovement 와 같은 누적 패턴).
 *
 * <p>월 급여 계산 때 {@code overtimeMinutes} 를 SUM 으로 집계해 연장수당을 산출한다.
 * {@code (employee_id, work_date)} UNIQUE 로 하루 중복 기록을 막는다.
 *
 * <p>단순화: 휴게시간 제외 없이 출근~퇴근 전체를 근무로 본다. 소정근로(8h=480분) 초과분이 연장.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "attendance")
public class Attendance extends BaseEntity {

    /** 소정근로 — 하루 8시간(480분). 이를 넘는 근무가 연장근로. */
    public static final int STANDARD_DAILY_MINUTES = 480;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "clock_in", nullable = false)
    private LocalTime clockIn;

    @Column(name = "clock_out", nullable = false)
    private LocalTime clockOut;

    @Column(name = "worked_minutes", nullable = false)
    private int workedMinutes;

    @Column(name = "overtime_minutes", nullable = false)
    private int overtimeMinutes;

    public static Attendance create(Employee employee, LocalDate workDate,
                                    LocalTime clockIn, LocalTime clockOut) {
        if (employee == null) throw new IllegalArgumentException("employee 는 null 일 수 없다.");
        if (workDate == null) throw new IllegalArgumentException("workDate 는 null 일 수 없다.");
        if (clockIn == null || clockOut == null)
            throw new IllegalArgumentException("clockIn/clockOut 은 null 일 수 없다.");
        if (!clockOut.isAfter(clockIn))
            throw new IllegalArgumentException("clockOut 은 clockIn 이후여야 한다.");

        Attendance a = new Attendance();
        a.employee = employee;
        a.workDate = workDate;
        a.clockIn = clockIn;
        a.clockOut = clockOut;
        a.workedMinutes = (int) Duration.between(clockIn, clockOut).toMinutes();
        a.overtimeMinutes = Math.max(0, a.workedMinutes - STANDARD_DAILY_MINUTES);
        return a;
    }
}
