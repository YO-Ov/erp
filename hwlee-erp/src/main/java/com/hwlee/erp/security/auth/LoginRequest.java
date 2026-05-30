package com.hwlee.erp.security.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "사용자명(이메일)은 필수입니다.") String username,
        @NotBlank(message = "비밀번호는 필수입니다.") String password
) {
}
