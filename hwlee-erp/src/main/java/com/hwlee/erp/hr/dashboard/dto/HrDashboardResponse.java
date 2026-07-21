package com.hwlee.erp.hr.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 인사(HR) 대시보드 집계 응답.
 *
 * <p>목록 API 를 클라이언트에서 합산하면 페이징 때문에 부정확하므로, 서버에서 정확히 집계해 내려준다.
 *
 * <p><b>사원 상태</b> — Employee 는 별도의 재직/퇴직 enum 이 없다. 상속한 {@code MasterStatus}
 * (ACTIVE/INACTIVE/BLOCKED)를 재직 판단에 쓴다. 재직 = ACTIVE, 퇴직/보류는 INACTIVE·BLOCKED 또는
 * soft-delete(deletedAt) 로 표현된다. soft-delete 된 사원은 @SQLRestriction 으로 조회에서 제외되므로
 * {@code totalEmployeeCount} 에도 들어가지 않는다("살아있는 사원" 총수).
 *
 * <p><b>급여대장</b> — PayrollRun 은 귀속월(period, "YYYY-MM")에 UNIQUE — 한 달에 급여대장 하나.
 * 따라서 이번 달 지표는 상태별 '건수'가 아니라 그 달 단일 급여대장의 상태 하나로 표현한다
 * ({@code thisMonthPayrollStatus}, 없으면 null). 상태별 건수 분포는 전체 기간 기준
 * {@code payrollStatusPipeline} 로 제공한다.
 */
public record HrDashboardResponse(
        long activeEmployeeCount,          // 재직(MasterStatus.ACTIVE) 사원 수
        long totalEmployeeCount,           // 전체 사원 수(soft-delete 제외 = 살아있는 사원)
        long departmentCount,              // 부서 수(soft-delete 제외)
        String thisMonthPeriod,            // 이번 달 귀속월 "YYYY-MM"
        String thisMonthPayrollStatus,     // 이번 달 급여대장 상태(DRAFT/CONFIRMED/PAID), 미생성 시 null
        BigDecimal thisMonthPayrollNet,    // 이번 달 급여대장 실지급 총액, 미생성 시 0
        Map<String, Long> payrollStatusPipeline, // 급여대장 상태명 → 건수(전체 기간, DRAFT/CONFIRMED/PAID 순)
        Map<String, Long> departmentHeadcount,   // 부서명 → 재직/재적 인원 수(인원 많은 순)
        List<RecentHire> recentHires) {          // 최근 입사자 5명

    public record RecentHire(
            String code,
            String name,
            String departmentName,
            LocalDate hireDate) {
    }
}
