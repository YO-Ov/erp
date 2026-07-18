package com.hwlee.erp.security.admin.dto;

import java.util.List;

/**
 * 관리자 화면용 사용자 응답 — 로그인정보 + 부여된 역할.
 *
 * <p>⚠️ passwordHash 는 절대 노출하지 않는다. 화면이 쓰는 것만 담는다.
 */
public record AdminUserResponse(
        Long id,
        String username,
        String employeeName,
        boolean enabled,
        boolean accountLocked,
        List<RoleRef> roles) {

    /** 사용자에게 부여된 역할 요약(역할 편집 체크박스와 맞춤). */
    public record RoleRef(Long id, String code, String name) {
    }
}
