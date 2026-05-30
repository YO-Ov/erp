package com.hwlee.erp.security.auth;

import com.hwlee.erp.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 로그인 인증 + JWT 발급.
 * AuthenticationManager 가 ErpUserDetailsService + PasswordEncoder 로 자격증명을 검증한다.
 * 검증 실패 시 AuthenticationException 이 던져지고 GlobalExceptionHandler 가 401 로 변환한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        List<String> roleCodes = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(ROLE_PREFIX))
                .map(a -> a.substring(ROLE_PREFIX.length()))
                .toList();

        String token = jwtTokenProvider.createToken(username, roleCodes);
        return new LoginResponse(token, "Bearer", jwtTokenProvider.getValiditySeconds(), roleCodes);
    }
}
