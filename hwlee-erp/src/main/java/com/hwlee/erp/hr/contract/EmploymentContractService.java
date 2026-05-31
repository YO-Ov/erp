package com.hwlee.erp.hr.contract;

import com.hwlee.erp.hr.contract.dto.EmploymentContractCreateRequest;
import com.hwlee.erp.hr.contract.dto.EmploymentContractResponse;
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
public class EmploymentContractService {

    private final EmploymentContractRepository repository;
    private final EmployeeRepository employeeRepository;
    private final EmploymentContractMapper mapper;

    /**
     * 새 급여계약 발효. 같은 직원의 직전 열린 계약이 있으면 새 발효일 전날로 자동 종료한다
     * — "한 시점에 유효 계약은 하나" 불변식을 유지.
     */
    @Transactional
    public EmploymentContractResponse create(EmploymentContractCreateRequest req) {
        Employee employee = employeeRepository.findById(req.employeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found: id=" + req.employeeId()));

        repository.findByEmployeeIdAndStatusAndEffectiveToIsNull(employee.getId(), ContractStatus.ACTIVE)
                .ifPresent(prev -> {
                    if (!req.effectiveFrom().isAfter(prev.getEffectiveFrom())) {
                        throw new IllegalArgumentException(
                                "새 계약 발효일은 직전 계약 발효일(" + prev.getEffectiveFrom() + ") 이후여야 합니다.");
                    }
                    prev.terminate(req.effectiveFrom().minusDays(1));
                });

        EmploymentContract contract = EmploymentContract.create(
                employee, req.position(), req.baseSalary(), req.contractedHours(), req.effectiveFrom());
        return mapper.toResponse(repository.save(contract));
    }

    public EmploymentContractResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    public List<EmploymentContractResponse> findByEmployee(Long employeeId) {
        return repository.findByEmployeeIdOrderByEffectiveFromDesc(employeeId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public EmploymentContractResponse terminate(Long id, LocalDate effectiveTo) {
        EmploymentContract contract = getOrThrow(id);
        contract.terminate(effectiveTo);
        return mapper.toResponse(contract);
    }

    EmploymentContract getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("EmploymentContract not found: id=" + id));
    }
}
