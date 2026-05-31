package com.hwlee.erp.hr.attendance;

import com.hwlee.erp.hr.attendance.dto.AttendanceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "employee.name", target = "employeeName")
    AttendanceResponse toResponse(Attendance entity);
}
