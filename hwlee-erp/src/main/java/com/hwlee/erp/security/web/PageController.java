package com.hwlee.erp.security.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 기본 화면 진입점 (Thymeleaf). 로그인 폼과 로그인 후 메뉴.
 * /login 은 permitAll, / 는 인증 필요(쿠키 토큰).
 */
@Controller
public class PageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String dashboard() {
        return "dashboard";
    }
}
