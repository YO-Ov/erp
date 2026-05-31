package com.hwlee.erp.hr.payroll.dto;

import java.math.BigDecimal;

public record PayslipResponse(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        BigDecimal basePay,
        BigDecimal overtimePay,
        BigDecimal grossPay,
        BigDecimal incomeTax,
        BigDecimal insuranceEmployee,
        BigDecimal insuranceCompany,
        BigDecimal totalDeduction,
        BigDecimal netPay
) {}
