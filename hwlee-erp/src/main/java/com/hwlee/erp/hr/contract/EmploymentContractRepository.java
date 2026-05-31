package com.hwlee.erp.hr.contract;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmploymentContractRepository extends JpaRepository<EmploymentContract, Long> {

    List<EmploymentContract> findByEmployeeIdOrderByEffectiveFromDesc(Long employeeId);

    /** 한 직원의 현재 열린(ACTIVE, effective_to NULL) 계약 — 새 계약 발효 시 닫을 대상. */
    Optional<EmploymentContract> findByEmployeeIdAndStatusAndEffectiveToIsNull(
            Long employeeId, ContractStatus status);

    /**
     * 주어진 일자에 유효한 모든 ACTIVE 계약 — 급여 계산의 출발점.
     * (effective_from ≤ date) AND (effective_to IS NULL OR effective_to ≥ date).
     */
    @Query("SELECT c FROM EmploymentContract c JOIN FETCH c.employee "
            + "WHERE c.status = com.hwlee.erp.hr.contract.ContractStatus.ACTIVE "
            + "AND c.effectiveFrom <= :date "
            + "AND (c.effectiveTo IS NULL OR c.effectiveTo >= :date) "
            + "ORDER BY c.employee.id ASC")
    List<EmploymentContract> findEffectiveOn(@Param("date") LocalDate date);
}
