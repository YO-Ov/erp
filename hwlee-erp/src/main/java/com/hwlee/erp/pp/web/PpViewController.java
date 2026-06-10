package com.hwlee.erp.pp.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * PP(생산) 화면 진입점 (Thymeleaf).
 *
 * <p>SD/MM/FI/HR 과 동일하게 화면은 껍데기 HTML 만 내려주고, 데이터는 각 페이지의 JS 가
 * REST API(/api/boms, /api/production-orders)를 호출해 채운다. 인가는 PRODUCTION/ADMIN.
 */
@Controller
@PreAuthorize("hasAnyRole('PRODUCTION','ADMIN')")
public class PpViewController {

    // ── BOM(자재 명세서) ────────────────────────────────────────
    @GetMapping("/pp/boms")
    public String bomList() {
        return "pp/bom/list";
    }

    // ── 계획오더(PlannedOrder, MRP) ─────────────────────────────
    @GetMapping("/pp/planned-orders")
    public String plannedList() {
        return "pp/planning/list";
    }

    // ── 생산지시(ProductionOrder) ───────────────────────────────
    @GetMapping("/pp/production-orders")
    public String productionList() {
        return "pp/order/list";
    }

    @GetMapping("/pp/production-orders/new")
    public String productionNew() {
        return "pp/order/form";
    }

    @GetMapping("/pp/production-orders/{id}")
    public String productionDetail(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "pp/order/detail";
    }
}
