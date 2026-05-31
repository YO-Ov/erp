package com.hwlee.erp.hr.payroll.dto;

import com.hwlee.erp.hr.payroll.PayrollStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PayrollRunResponse(
        Long id,
        String number,
        String period,
        LocalDate runDate,
        PayrollStatus status,
        BigDecimal totalGross,
        BigDecimal totalDeduction,
        BigDecimal totalNet,
        LocalDateTime confirmedAt,
        LocalDateTime paidAt,
        List<PayslipResponse> payslips,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
