package com.hwlee.mes.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * MES 현장 화면 라우팅 — 데이터는 JS 가 /api/* 를 호출해 채운다.
 */
@Controller
public class MesViewController {

    @GetMapping("/")
    public String home() {
        return "redirect:/work-orders";
    }

    @GetMapping("/work-orders")
    public String list() {
        return "workorder/list";
    }

    @GetMapping("/work-orders/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "workorder/detail";
    }
}
