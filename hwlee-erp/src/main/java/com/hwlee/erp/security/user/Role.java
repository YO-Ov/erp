package com.hwlee.erp.security.user;

import com.hwlee.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * 역할(Role) — SALES/PURCHASING/FINANCE/ADMIN.
 * code 는 {@code ROLE_} 접두어 없이 저장하고, UserDetails 구성 시 접두어를 붙인다
 * (Spring Security 의 {@code hasRole('FINANCE')} 규약).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "role")
public class Role extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    public static Role create(String code, String name) {
        Role r = new Role();
        r.code = code;
        r.name = name;
        return r;
    }
}
