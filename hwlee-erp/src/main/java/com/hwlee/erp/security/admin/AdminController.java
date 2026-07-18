package com.hwlee.erp.security.admin;

import com.hwlee.erp.security.admin.dto.AdminRoleResponse;
import com.hwlee.erp.security.admin.dto.AdminUserResponse;
import com.hwlee.erp.security.admin.dto.UpdateRolesRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 REST — 사용자 목록/역할 조회, 사용자 역할 교체. ADMIN 전용.
 *
 * <p>기존 Thymeleaf 화면({@link AdminViewController})을 React 로 옮기며 신설.
 * 로직은 기존 {@link AppUserAdminService} 를 그대로 재사용한다(엔티티→DTO 변환만 추가).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AppUserAdminService service;

    @GetMapping("/users")
    public List<AdminUserResponse> users() {
        return service.listUsers();
    }

    @GetMapping("/roles")
    public List<AdminRoleResponse> roles() {
        return service.listRoles();
    }

    /** 사용자 역할 통째 교체 — 선택된 roleIds 로. 비면 모든 역할 회수. */
    @PutMapping("/users/{id}/roles")
    public ResponseEntity<Void> updateRoles(@PathVariable Long id,
                                            @Valid @RequestBody UpdateRolesRequest req) {
        service.replaceRoles(id, req.roleIds());
        return ResponseEntity.noContent().build();
    }
}
