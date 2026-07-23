package com.hwlee.erp.security.auth;

import java.util.List;

/**
 * 현재 로그인 사용자 정보. GET /api/auth/me 응답.
 * 앱 부팅 시 저장된 토큰이 아직 유효한지 검증하는 용도 —
 * 토큰이 만료/무효면 이 요청이 401 로 떨어져 프론트가 로그인 화면으로 보낸다.
 */
public record MeResponse(
        String username,
        List<String> roles
) {
}
