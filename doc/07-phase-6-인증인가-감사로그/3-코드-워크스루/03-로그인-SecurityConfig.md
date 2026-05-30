# Phase 6 코드 워크스루 ③ — 로그인 API와 SecurityConfig: 필터 체인의 조립

> 대상 파일
> - `security/config/SecurityConfig.java`
> - `security/auth/AuthController.java` / `AuthService.java` / `LoginRequest.java` / `LoginResponse.java`
>
> 이 글의 목표: Spring Security의 **필터 체인을 어떻게 구성**하고, 로그인 시 `AuthenticationManager`가 어떻게 비밀번호를 검증해 토큰을 발급하는지 본다. 브리핑 §3-3(로그인 흐름)·§3-4(BCrypt)가 코드가 되는 지점.

---

## 0. 한눈에

```
SecurityConfig
 ├─ PasswordEncoder = BCrypt           (해시 비교기)
 ├─ AuthenticationManager              (UserDetailsService + PasswordEncoder 조립)
 └─ SecurityFilterChain
       ├─ csrf 비활성 + STATELESS 세션
       ├─ permitAll: /api/auth/login, /login, /swagger, /api/health, 정적
       ├─ 그 외: authenticated()
       ├─ EntryPoint(401) / AccessDeniedHandler(403)
       └─ JwtAuthenticationFilter 를 인증 필터 앞에 삽입

AuthController.login → AuthService.login
       └─ authenticationManager.authenticate(username, password)  ← 여기서 BCrypt 비교
       └─ JwtTokenProvider.createToken(...)
       └─ 헤더 JSON + HttpOnly 쿠키로 토큰 반환
```

---

## 1. `SecurityConfig` — 보안의 조립도

```java
@Configuration
@EnableMethodSecurity                                    // ← @PreAuthorize 활성화
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    private static final String[] PUBLIC_PATHS = {
            "/api/auth/login", "/api/auth/logout",
            "/api/health",
            "/login", "/css/**", "/js/**", "/favicon.ico", "/error",
            "/swagger-ui.html", "/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**"
    };
```

- **`@EnableMethodSecurity`**: 이 한 줄이 `@PreAuthorize` 를 켠다. 없으면 컨트롤러에 붙인 `@PreAuthorize` 가 **조용히 무시**된다(인가가 통째로 안 걸리는 위험한 버그). 워크스루 ⑥의 인가가 작동하는 전제.
- **`PUBLIC_PATHS`**: 인증 없이 통과시킬 경로. 로그인 자체(`/api/auth/login`)는 인증 전이라 당연히 열려야 하고, Swagger·헬스체크·로그인 화면·정적 리소스도 공개.

### 1-1. PasswordEncoder + AuthenticationManager

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

@Bean
public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
}
```

- **`BCryptPasswordEncoder`**: 브리핑 §3-4의 BCrypt 해시기. 우리가 해시 알고리즘을 직접 구현하지 않는다 — 등록만 하면 로그인 검증과 시드 비밀번호 모두 이걸 쓴다.
- **`AuthenticationManager`**: 우리가 직접 만들지 않고 Spring이 조립한 걸 꺼내 빈으로 노출한다. Spring은 클래스패스에 있는 `UserDetailsService`(우리의 `ErpUserDetailsService`)와 `PasswordEncoder`(BCrypt)를 자동으로 묶어 `DaoAuthenticationProvider`를 구성한다 → "username으로 UserDetails 로딩 → 저장된 해시와 입력 비밀번호를 BCrypt로 비교"가 공짜로 완성된다.

### 1-2. 필터 체인

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_PATHS).permitAll()
                    .anyRequest().authenticated())
            .exceptionHandling(e -> e
                    .authenticationEntryPoint(authenticationEntryPoint)   // 401
                    .accessDeniedHandler(accessDeniedHandler))            // 403
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                    UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

한 줄씩의 의도:

- **`csrf.disable()`**: CSRF 토큰은 세션-쿠키 기반 폼 제출을 노린 공격을 막는 장치다. 우리는 무상태 JWT라서 표준 CSRF 토큰 메커니즘을 끄고, 쿠키 방식의 위험은 `SameSite=Strict` 쿠키로 1차 방어한다(설계 §7-1의 절충 — 학습 범위라 한계 명시).
- **`STATELESS`**: "서버 세션을 만들지 마라". 브리핑 §3-1의 무상태 핵심. 세션을 안 만드니 `JSESSIONID`도 없고, 매 요청은 토큰만으로 판단된다.
- **`permitAll` → `anyRequest().authenticated()`**: 화이트리스트 방식. 명시적으로 연 경로 외에는 **전부 인증 필요**. "기본은 잠그고, 열 것만 연다"가 보안의 정석(브리핑 §1의 "구멍 막기").
- **`addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)`**: 우리 JWT 필터(워크스루 ①)를 Security 표준 인증 필터 **앞에** 끼운다. 그래야 컨트롤러/인가 검사 시점엔 이미 `SecurityContext`에 사용자가 채워져 있다.

---

## 2. 로그인 흐름 — `AuthController` → `AuthService`

### 2-1. DTO

```java
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password) {}

public record LoginResponse(
        String accessToken, String tokenType, long expiresIn, List<String> roles) {}
```

요청은 username/password, 응답은 토큰 + 타입(Bearer) + 만료초 + 역할 목록(화면이 메뉴 구성에 쓸 수 있게).

### 2-2. `AuthService` — 인증 위임 + 토큰 발급

```java
@Service
public class AuthService {
    private static final String ROLE_PREFIX = "ROLE_";
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));   // ① 비밀번호 검증

        List<String> roleCodes = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(ROLE_PREFIX))      // ROLE_SALES 만 골라
                .map(a -> a.substring(ROLE_PREFIX.length())) // → SALES (접두어 제거)
                .toList();

        String token = jwtTokenProvider.createToken(username, roleCodes);  // ② 토큰 발급
        return new LoginResponse(token, "Bearer", jwtTokenProvider.getValiditySeconds(), roleCodes);
    }
}
```

- **①** `authenticate(...)`가 핵심. 안에서 `ErpUserDetailsService.loadUserByUsername`(워크스루 ②)으로 사용자를 불러오고, `BCryptPasswordEncoder.matches(입력, 저장해시)`로 비밀번호를 대조한다. **불일치/비활성/잠금이면 여기서 `AuthenticationException`을 던진다** → 컨트롤러까지 전파돼 `GlobalExceptionHandler`가 401로 변환(워크스루 ⑥).
- **②** 검증을 통과한 권한에서 `ROLE_` 접두어를 떼어 토큰에 `SALES` 형태로 굽는다. 토큰을 저장할 땐 접두어 없이, 검증할 땐 다시 붙이는(워크스루 ①의 필터) 규약의 짝.
- 비밀번호 비교를 우리가 직접 안 한다 — `if (encoder.matches(...))` 한 줄도 안 쓴다. `AuthenticationManager`에 위임하는 게 표준이고, 계정 상태(enabled/locked) 검사까지 한 번에 따라온다.

### 2-3. `AuthController` — 헤더 + 쿠키 동시 발급

```java
@PostMapping("/login")
public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
    LoginResponse result = authService.login(request.username(), request.password());

    ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.COOKIE_NAME, result.accessToken())
            .httpOnly(true)        // JS 접근 차단 (XSS 완화)
            .path("/")
            .sameSite("Strict")    // CSRF 1차 방어
            .maxAge(result.expiresIn())
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    return result;                 // 본문엔 토큰 JSON (API 클라이언트용)
}
```

- **응답 본문(JSON)과 쿠키 둘 다**에 토큰을 싣는다. API 클라이언트는 본문의 `accessToken`을 꺼내 헤더에 쓰고, 브라우저는 쿠키를 자동 보관 → 워크스루 ①의 "필터가 헤더·쿠키 둘 다 본다"와 짝을 이룬다.
- **`httpOnly(true)`**: JavaScript에서 `document.cookie`로 토큰을 읽지 못하게 막는다 — XSS로 토큰이 탈취될 위험을 줄인다.
- 로그아웃은 같은 이름의 쿠키를 `maxAge(0)`로 덮어 즉시 만료시킨다(무상태라 서버 측 토큰 폐기는 없음 — 한계).

---

## 3. 누적성

- **Phase 2의 HTTP 상태 의미**가 401(로그인 실패)로 확장된다.
- `AuthenticationManager`가 쓰는 `UserDetailsService`는 워크스루 ②의 `ErpUserDetailsService`, 토큰 발급은 워크스루 ①의 `JwtTokenProvider` — 이 글은 그 둘을 **조립**하는 자리다.

---

## 4. 자기 점검

- [ ] `@EnableMethodSecurity`를 빠뜨리면 무슨 일이 생기는가? (힌트: `@PreAuthorize`)
- [ ] 비밀번호 비교(`encoder.matches`)를 우리 코드가 직접 하지 않는데, 어디서 일어나는가?
- [ ] 토큰을 본문과 쿠키에 둘 다 싣는 이유는?
- [ ] `STATELESS` 세션 정책이 의미하는 바는?
- [ ] 로그인 실패가 401로 응답되는 경로는? (다음 워크스루 ⑥와 연결)

---

## 5. 한 줄 요약

> `SecurityConfig`는 BCrypt·AuthenticationManager·필터 체인을 조립하고 "기본 잠금, 화이트리스트만 개방"을 선언한다. 로그인은 `AuthenticationManager`에 비밀번호 검증을 위임하고, 통과하면 역할을 토큰에 구워 헤더+쿠키로 돌려준다.
