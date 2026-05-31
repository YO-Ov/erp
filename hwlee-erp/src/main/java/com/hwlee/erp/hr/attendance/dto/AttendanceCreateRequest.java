package com.hwlee.erp.hr.attendance.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record AttendanceCreateRequest(
        @NotNull Long employeeId,
        @NotNull LocalDate workDate,
        @NotNull LocalTime clockIn,
        @NotNull LocalTime clockOut
) {}
