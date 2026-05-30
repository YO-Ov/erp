package com.hwlee.erp.security.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 관리자 화면 (Thymeleaf) — 사용자 목록/역할 부여, 역할-권한 조회. ADMIN 전용.
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminViewController {

    private final AppUserAdminService service;

    @GetMapping("/admin/users")
    public String users(Model model) {
        model.addAttribute("users", service.findAllUsers());
        model.addAttribute("allRoles", service.findAllRoles());
        return "admin/users";
    }

    @PostMapping("/admin/users/{id}/roles")
    public String updateRoles(@PathVariable Long id,
                              @RequestParam(name = "roleIds", required = false) List<Long> roleIds) {
        service.replaceRoles(id, roleIds);
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/roles")
    public String roles(Model model) {
        model.addAttribute("roles", service.findAllRoles());
        return "admin/roles";
    }
}
