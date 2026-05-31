package com.hwlee.erp.hr.attendance;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    boolean existsByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    List<Attendance> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
            Long employeeId, LocalDate from, LocalDate to);

    /** 한 직원의 기간 내 연장근로 분 합계 — 급여 계산용. 데이터 없으면 0. */
    @Query("SELECT COALESCE(SUM(a.overtimeMinutes), 0) FROM Attendance a "
            + "WHERE a.employee.id = :employeeId "
            + "AND a.workDate BETWEEN :from AND :to")
    int sumOvertimeMinutes(@Param("employeeId") Long employeeId,
                           @Param("from") LocalDate from,
                           @Param("to") LocalDate to);
}
