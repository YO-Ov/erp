package com.hwlee.erp.security.admin.dto;

import java.util.List;

/**
 * 사용자 역할 교체 요청 — 선택된 역할 id 집합으로 통째 교체(비면 모든 역할 회수).
 */
public record UpdateRolesRequest(List<Long> roleIds) {
}
