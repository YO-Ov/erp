package com.hwlee.erp.security.auth;

import com.hwlee.erp.security.jwt.JwtAuthenticationFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "로그인/로그아웃 (JWT 발급)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인", description = "이메일/비밀번호로 JWT 발급. 헤더(Bearer)와 HttpOnly 쿠키 둘 다 사용 가능")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResponse result = authService.login(request.username(), request.password());
        // 브라우저용: HttpOnly + SameSite 쿠키로도 발급 (XSS 완화, JS 접근 차단)
        ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.COOKIE_NAME, result.accessToken())
                .httpOnly(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(result.expiresIn())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return result;
    }

    @Operation(summary = "로그아웃", description = "쿠키 토큰 제거 (무상태이므로 서버 측 폐기는 없음)")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
    }
}
