package com.hwlee.erp.approval;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 전자결재 화면 — 상신함/결재함. 모든 로그인 사용자가 접근한다(내 문서만 조회).
 */
@Controller
@PreAuthorize("isAuthenticated()")
public class ApprovalViewController {

    @GetMapping("/approvals")
    public String list() {
        return "approval/list";
    }
}
