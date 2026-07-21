package com.hwlee.erp.master.employee;

import com.hwlee.erp.common.entity.MasterStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByCode(String code);

    boolean existsByEmail(String email);

    // ── 인사 대시보드 집계 ──

    /** 상태별 사원 수 (재직 = MasterStatus.ACTIVE). */
    long countByStatus(MasterStatus status);

    /** 부서별 인원 수 — 인원 많은 순. */
    @Query("select e.department.name as name, count(e) as count "
            + "from Employee e group by e.department.name order by count(e) desc")
    List<DeptHeadcount> aggregateHeadcountByDepartment();

    /** 최근 입사자 5명. */
    List<Employee> findTop5ByOrderByHireDateDescIdDesc();
}
