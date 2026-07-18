package com.hwlee.erp.security.admin.dto;

import java.util.List;

/**
 * 관리자 화면용 역할 응답 — 역할 + 그 역할이 가진 권한 코드 목록.
 */
public record AdminRoleResponse(
        Long id,
        String code,
        String name,
        List<String> permissions) {
}
