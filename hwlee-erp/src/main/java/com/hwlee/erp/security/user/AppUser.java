package com.hwlee.erp.security.user;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.employee.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * 로그인 계정(AppUser) — 직원(Employee)과 1:1.
 * 인사정보(Employee)와 로그인정보(username/passwordHash/역할)를 분리한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "app_user")
public class AppUser extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, unique = true, length = 200)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    public static AppUser create(Employee employee, String username, String passwordHash) {
        AppUser u = new AppUser();
        u.employee = employee;
        u.username = username;
        u.passwordHash = passwordHash;
        u.enabled = true;
        u.accountLocked = false;
        return u;
    }

    /** 역할 부여 (이미 있으면 무시) */
    public void grantRole(Role role) {
        this.roles.add(role);
    }

    /** 역할 회수 */
    public void revokeRole(Role role) {
        this.roles.removeIf(r -> r.getId() != null && r.getId().equals(role.getId()));
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void changeEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
