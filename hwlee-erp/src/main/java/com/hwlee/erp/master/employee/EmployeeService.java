package com.hwlee.erp.master.employee;

import com.hwlee.erp.common.code.CodeGenerator;
import com.hwlee.erp.master.department.Department;
import com.hwlee.erp.master.department.DepartmentRepository;
import com.hwlee.erp.master.employee.dto.EmployeeCreateRequest;
import com.hwlee.erp.master.employee.dto.EmployeeResponse;
import com.hwlee.erp.master.employee.dto.EmployeeUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    static final String CODE_PREFIX = "EMP";

    private final EmployeeRepository repository;
    private final DepartmentRepository departmentRepository;
    private final CodeGenerator codeGenerator;

    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest req) {
        if (repository.existsByEmail(req.email())) {
            throw new IllegalStateException("이미 등록된 이메일입니다: " + req.email());
        }
        Department department = resolveDepartment(req.departmentCode());
        String code = codeGenerator.nextCode(CODE_PREFIX);
        Employee employee = Employee.create(code, req.name(), req.email(), department, req.hireDate());
        return toResponse(repository.save(employee));
    }

    public EmployeeResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    public EmployeeResponse findByCode(String code) {
        return toResponse(repository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found: code=" + code)));
    }

    public List<EmployeeResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeUpdateRequest req) {
        Employee employee = getOrThrow(id);
        Department department = resolveDepartment(req.departmentCode());
        employee.update(req.name(), req.email(), department, req.hireDate());
        return toResponse(employee);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    private Employee getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found: id=" + id));
    }

    private Department resolveDepartment(String code) {
        return departmentRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Department not found: code=" + code));
    }

    private EmployeeResponse toResponse(Employee e) {
        Department d = e.getDepartment();
        return new EmployeeResponse(
                e.getId(),
                e.getCode(),
                e.getName(),
                e.getEmail(),
                d.getCode(),
                d.getName(),
                e.getHireDate(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getCreatedBy(),
                e.getUpdatedAt(),
                e.getUpdatedBy()
        );
    }
}
