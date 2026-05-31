package com.hwlee.erp.hr.attendance;

import com.hwlee.erp.hr.attendance.dto.AttendanceCreateRequest;
import com.hwlee.erp.hr.attendance.dto.AttendanceResponse;
import com.hwlee.erp.master.employee.Employee;
import com.hwlee.erp.master.employee.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository repository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceMapper mapper;

    @Transactional
    public AttendanceResponse create(AttendanceCreateRequest req) {
        Employee employee = employeeRepository.findById(req.employeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found: id=" + req.employeeId()));
        if (repository.existsByEmployeeIdAndWorkDate(employee.getId(), req.workDate())) {
            throw new IllegalArgumentException(
                    "이미 등록된 근태입니다. employeeId=" + employee.getId() + ", workDate=" + req.workDate());
        }
        Attendance attendance = Attendance.create(employee, req.workDate(), req.clockIn(), req.clockOut());
        return mapper.toResponse(repository.save(attendance));
    }

    public List<AttendanceResponse> findByEmployee(Long employeeId, LocalDate from, LocalDate to) {
        return repository.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(employeeId, from, to).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
