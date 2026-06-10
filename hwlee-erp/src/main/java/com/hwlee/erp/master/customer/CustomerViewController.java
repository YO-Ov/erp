package com.hwlee.erp.master.customer;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 고객 마스터 화면 — 영업이 기본정보를 등록·수정한다. 신용한도는 화면에 표시만 하고
 * 변경은 여신(재무) 흐름으로 유도한다(권한 분리).
 */
@Controller
@PreAuthorize("hasAnyRole('SALES','ADMIN')")
public class CustomerViewController {

    @GetMapping("/master/customers")
    public String list() {
        return "master/customer/list";
    }

    @GetMapping("/master/customers/new")
    public String create() {
        return "master/customer/form";
    }

    @GetMapping("/master/customers/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "master/customer/detail";
    }

    @GetMapping("/master/customers/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "master/customer/form";
    }
}
