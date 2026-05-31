package com.hwlee.erp.hr.contract.dto;

import com.hwlee.erp.hr.contract.ContractStatus;
import com.hwlee.erp.hr.contract.Position;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmploymentContractResponse(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        Position position,
        BigDecimal baseSalary,
        int contractedHours,
        BigDecimal hourlyWage,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        ContractStatus status,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
