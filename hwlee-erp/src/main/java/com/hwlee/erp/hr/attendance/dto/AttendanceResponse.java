package com.hwlee.erp.hr.attendance.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AttendanceResponse(
        Long id,
        Long employeeId,
        String employeeName,
        LocalDate workDate,
        LocalTime clockIn,
        LocalTime clockOut,
        int workedMinutes,
        int overtimeMinutes,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
