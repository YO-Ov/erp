package com.hwlee.erp.hr.dashboard;

import com.hwlee.erp.common.entity.MasterStatus;
import com.hwlee.erp.hr.dashboard.dto.HrDashboardResponse;
import com.hwlee.erp.hr.dashboard.dto.HrDashboardResponse.RecentHire;
import com.hwlee.erp.hr.payroll.PayrollRun;
import com.hwlee.erp.hr.payroll.PayrollRunRepository;
import com.hwlee.erp.hr.payroll.PayrollStatus;
import com.hwlee.erp.master.department.DepartmentRepository;
import com.hwlee.erp.master.employee.EmployeeRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 인사(HR) 대시보드 집계 — 사원/부서/급여대장을 상태별로 서버에서 정확히 합산한다. */
@Service
@RequiredArgsConstructor
public class HrDashboardService {

    private final EmployeeRepository employeeRepo;
    private final DepartmentRepository departmentRepo;
    private final PayrollRunRepository payrollRunRepo;

    @Transactional(readOnly = true)
    public HrDashboardResponse summary() {
        // 1) 사원 — 재직(ACTIVE) 수 + 살아있는 사원 총수.
        long activeEmployees = employeeRepo.countByStatus(MasterStatus.ACTIVE);
        long totalEmployees = employeeRepo.count();

        // 2) 부서 수.
        long departmentCount = departmentRepo.count();

        // 3) 이번 달 급여대장 — 귀속월 UNIQUE 라 한 달에 하나. 있으면 그 상태/실지급액.
        String period = YearMonth.now().toString(); // "YYYY-MM"
        String thisMonthStatus = null;
        BigDecimal thisMonthNet = BigDecimal.ZERO;
        PayrollRun thisMonthRun = payrollRunRepo.findByPeriod(period).orElse(null);
        if (thisMonthRun != null) {
            thisMonthStatus = thisMonthRun.getStatus().name();
            thisMonthNet = thisMonthRun.getTotalNet();
        }

        // 4) 급여대장 상태별 건수(전체 기간) — enum 정의 순서 유지.
        Map<String, Long> payrollPipeline = new LinkedHashMap<>();
        for (PayrollStatus s : PayrollStatus.values()) {
            payrollPipeline.put(s.name(), payrollRunRepo.countByStatus(s));
        }

        // 5) 보조 — 부서별 인원 수(인원 많은 순), 최근 입사자 5명.
        Map<String, Long> departmentHeadcount = new LinkedHashMap<>();
        employeeRepo.aggregateHeadcountByDepartment()
                .forEach(row -> departmentHeadcount.put(row.getName(), row.getCount()));

        List<RecentHire> recentHires = employeeRepo.findTop5ByOrderByHireDateDescIdDesc().stream()
                .map(e -> new RecentHire(e.getCode(), e.getName(),
                        e.getDepartment().getName(), e.getHireDate()))
                .toList();

        return new HrDashboardResponse(
                activeEmployees, totalEmployees, departmentCount,
                period, thisMonthStatus, thisMonthNet,
                payrollPipeline, departmentHeadcount, recentHires);
    }
}
