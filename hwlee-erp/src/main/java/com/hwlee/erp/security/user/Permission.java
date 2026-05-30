package com.hwlee.erp.security.user;

import com.hwlee.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 세부 권한(Permission) — 예: FI_POST, SD_WRITE.
 * 이번 Phase 의 인가는 역할(Role) 단위로 시작하며, permission 은 구조와 시드만 둔다.
 * (추후 {@code hasAuthority('FI_POST')} 처럼 권한 단위로 쪼갤 복선)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "permission")
public class Permission extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    public static Permission create(String code, String name) {
        Permission p = new Permission();
        p.code = code;
        p.name = name;
        return p;
    }
}
