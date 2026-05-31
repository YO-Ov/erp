package com.hwlee.erp.hr.contract;

import com.hwlee.erp.hr.contract.dto.EmploymentContractResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmploymentContractMapper {

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "employee.code", target = "employeeCode")
    @Mapping(source = "employee.name", target = "employeeName")
    @Mapping(expression = "java(entity.hourlyWage())", target = "hourlyWage")
    EmploymentContractResponse toResponse(EmploymentContract entity);
}
