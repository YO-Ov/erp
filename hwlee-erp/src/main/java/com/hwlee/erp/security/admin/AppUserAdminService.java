package com.hwlee.erp.security.admin;

import com.hwlee.erp.security.user.AppUser;
import com.hwlee.erp.security.user.AppUserRepository;
import com.hwlee.erp.security.user.Role;
import com.hwlee.erp.security.user.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자용 사용자/역할 관리 (Thymeleaf 화면 백엔드).
 * 부서 기반 시드로 만들어진 user_role 을 관리자가 화면에서 수동 조정(확정 ③)하는 통로.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppUserAdminService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;

    public List<AppUser> findAllUsers() {
        List<AppUser> users = appUserRepository.findAll();
        // OSIV=false 라 뷰 렌더링 시점엔 세션이 닫혀 있다 → 화면에서 쓸 지연 연관을
        // 트랜잭션(여기) 안에서 미리 초기화한다. (직원·역할)
        users.forEach(u -> {
            if (u.getEmployee() != null) {
                u.getEmployee().getName();
            }
            u.getRoles().size();
        });
        return users;
    }

    public List<Role> findAllRoles() {
        List<Role> roles = roleRepository.findAll();
        roles.forEach(role -> role.getPermissions().size()); // 권한 컬렉션 초기화
        return roles;
    }

    /** 사용자의 역할 집합을 통째로 교체 (체크박스 선택분으로). */
    @Transactional
    public void replaceRoles(Long userId, List<Long> roleIds) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("AppUser not found: id=" + userId));
        List<Role> roles = (roleIds == null || roleIds.isEmpty())
                ? List.of()
                : roleRepository.findAllById(roleIds);
        // 기존 전부 회수 후 선택분 부여 (단순·명확)
        user.getRoles().clear();
        roles.forEach(user::grantRole);
    }
}
