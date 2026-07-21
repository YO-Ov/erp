package com.hwlee.erp.hr.payroll;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {

    boolean existsByPeriod(String period);

    Optional<PayrollRun> findByPeriod(String period);

    /** 상태별 급여대장 건수 — 인사 대시보드 파이프라인용. */
    long countByStatus(PayrollStatus status);

    /** 명세 라인과 직원까지 fetch — 트랜잭션 밖 직렬화 시 LazyInitializationException 방지. */
    @Query("SELECT DISTINCT r FROM PayrollRun r "
            + "LEFT JOIN FETCH r.payslips p "
            + "LEFT JOIN FETCH p.employee "
            + "WHERE r.id = :id")
    Optional<PayrollRun> findByIdWithPayslips(@Param("id") Long id);
}
