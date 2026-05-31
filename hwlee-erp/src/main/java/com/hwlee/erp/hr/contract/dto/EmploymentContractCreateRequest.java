package com.hwlee.erp.hr.contract.dto;

import com.hwlee.erp.hr.contract.Position;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EmploymentContractCreateRequest(
        @NotNull Long employeeId,
        @NotNull Position position,
        @NotNull @Positive BigDecimal baseSalary,
        @Positive int contractedHours,
        @NotNull LocalDate effectiveFrom
) {}
