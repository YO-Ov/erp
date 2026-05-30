package com.hwlee.erp.security.auth;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        List<String> roles
) {
}
