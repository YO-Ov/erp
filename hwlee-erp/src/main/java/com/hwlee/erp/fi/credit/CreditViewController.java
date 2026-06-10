package com.hwlee.erp.fi.credit;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 여신 상향 요청 화면 — 영업(요청)과 재무(승인) 공용. 한 화면에서 역할에 따라 버튼이 달라진다.
 */
@Controller
@PreAuthorize("hasAnyRole('SALES','FINANCE','ADMIN')")
public class CreditViewController {

    @GetMapping("/fi/credit-limit-requests")
    public String list() {
        return "fi/credit/list";
    }
}
