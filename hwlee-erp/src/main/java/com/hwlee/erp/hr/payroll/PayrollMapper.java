package com.hwlee.erp.hr.payroll;

import com.hwlee.erp.hr.payroll.dto.PayrollRunResponse;
import com.hwlee.erp.hr.payroll.dto.PayslipResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PayrollMapper {

    PayrollRunResponse toResponse(PayrollRun entity);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "employee.code", target = "employeeCode")
    @Mapping(source = "employee.name", target = "employeeName")
    PayslipResponse toResponse(Payslip line);
}
