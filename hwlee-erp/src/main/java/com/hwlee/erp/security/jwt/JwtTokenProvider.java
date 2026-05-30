package com.hwlee.erp.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * JWT 생성/파싱/검증. 무상태(서버가 토큰을 기억하지 않음) — 서명으로만 신뢰.
 * payload 에 sub(username) 와 roles(역할 코드 목록)를 담는다. 민감정보는 넣지 않는다.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String ROLES_CLAIM = "roles";

    private final SecretKey key;
    private final long validitySeconds;

    public JwtTokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.validitySeconds = properties.accessTokenValiditySeconds();
    }

    /** 로그인 성공 시 토큰 발급 */
    public String createToken(String username, Collection<String> roleCodes) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(validitySeconds);
        return Jwts.builder()
                .subject(username)
                .claim(ROLES_CLAIM, List.copyOf(roleCodes))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public long getValiditySeconds() {
        return validitySeconds;
    }

    /** 서명/만료 검증. 실패하면 false (예외를 밖으로 던지지 않음) */
    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            log.debug("JWT 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public String getUsername(String token) {
        return parse(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Object roles = parse(token).get(ROLES_CLAIM);
        if (roles instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
