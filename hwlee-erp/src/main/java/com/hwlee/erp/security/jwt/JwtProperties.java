package com.hwlee.erp.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정 바인딩 (application.yml 의 erp.jwt.*).
 */
@ConfigurationProperties(prefix = "erp.jwt")
public record JwtProperties(
        String secret,
        long accessTokenValiditySeconds
) {
}
