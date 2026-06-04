package com.hwlee.erp.hr.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * HR(인사/급여) 화면 진입점 (Thymeleaf).
 *
 * <p>SD/MM/FI 와 동일하게 화면은 껍데기 HTML 만 내려주고, 실제 데이터는 각 페이지의 JS 가
 * REST API(/api/employees, /api/employment-contracts, /api/attendances, /api/payroll-runs)를
 * 호출해 채운다. 여기서는 라우팅과 권한만 담당한다. 인가는 REST 컨트롤러와 동일하게 HR/ADMIN.
 *
 * <p>HR 도메인 API 는 전역 목록이 없어(계약·근태는 employeeId 로만 조회) <b>직원 중심</b>으로 구성한다:
 * 직원 목록 → 직원 상세에서 그 직원의 급여계약·근태를 보고 등록한다.
 */
@Controller
@PreAuthorize("hasAnyRole('HR','ADMIN')")
public class HrViewController {

    // ── 직원(인사) — 급여계약·근태 진입점 ─────────────────────────
    @GetMapping("/hr/employees")
    public String employeeList() {
        return "hr/employee/list";
    }

    @GetMapping("/hr/employees/{id}")
    public String employeeDetail(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "hr/employee/detail";
    }

    // ── 급여대장(PayrollRun) ────────────────────────────────────
    //   생성(DRAFT, 자동계산) → 확정(인건비 전표) → 지급(지급 전표)
    @GetMapping("/hr/payroll-runs")
    public String payrollList() {
        return "hr/payroll/list";
    }

    @GetMapping("/hr/payroll-runs/new")
    public String payrollNew() {
        return "hr/payroll/form";
    }

    @GetMapping("/hr/payroll-runs/{id}")
    public String payrollDetail(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "hr/payroll/detail";
    }
}
