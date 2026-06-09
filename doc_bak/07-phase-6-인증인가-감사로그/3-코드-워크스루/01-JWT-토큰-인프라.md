# Phase 6 코드 워크스루 ① — JWT 토큰 인프라: 서명된 신분증을 발급·검증한다

> 대상 파일
> - `security/jwt/JwtProperties.java`
> - `security/jwt/JwtTokenProvider.java`
> - `security/jwt/JwtAuthenticationFilter.java`
>
> 이 글의 목표: **무상태(stateless) 인증의 심장** — 토큰을 어떻게 만들고(서명), 어떻게 까서(검증) "현재 사용자"를 복원하는지 이해한다. 브리핑 §3(JWT 무상태 토큰)이 코드가 되는 지점.

---

## 0. 한눈에

```
[로그인]  username/password  ──검증OK──▶  JwtTokenProvider.createToken()
                                              payload: {sub=username, roles=[SALES], exp}
                                              ← "eyJ...서명됨"

[보호된 요청]  Authorization: Bearer eyJ...  (또는 ACCESS_TOKEN 쿠키)
        │
        └─ JwtAuthenticationFilter (매 요청 1회)
             ├─ 헤더/쿠키에서 토큰 추출
             ├─ JwtTokenProvider.validate()  ← 서명·만료 검사
             └─ SecurityContext 에 "현재 사용자 = username + 권한" 세팅
```

핵심 한 줄: **서버는 아무것도 기억하지 않는다.** 신뢰의 근거는 오직 토큰에 찍힌 **서명**. 위조하면 서명이 깨져 거부된다.

---

## 1. `JwtProperties` — 설정을 타입 안전하게 바인딩

```java
@ConfigurationProperties(prefix = "erp.jwt")
public record JwtProperties(
        String secret,
        long accessTokenValiditySeconds
) {}
```

`application.yml` 의 이 부분과 1:1로 묶인다:

```yaml
erp:
  jwt:
    secret: ${JWT_SECRET:hwlee-erp-local-dev-secret-key-...}
    access-token-validity-seconds: 3600
```

- **`secret`**: 서명/검증에 쓰는 서버만 아는 비밀키. `${JWT_SECRET:...}` 는 "환경변수 `JWT_SECRET` 가 있으면 그걸, 없으면 뒤의 기본값"이라는 뜻 — 운영에선 환경변수로 주입하고, 로컬 학습에선 기본값으로 그냥 돈다.
- **왜 record + @ConfigurationProperties?** 문자열 키로 `@Value("${erp.jwt.secret}")` 를 여기저기 박는 대신, 설정을 **하나의 타입**으로 모아 오타·타입오류를 컴파일 타임에 잡는다. `SecurityConfig` 에서 `@EnableConfigurationProperties(JwtProperties.class)` 로 활성화한다.

---

## 2. `JwtTokenProvider` — 발급과 검증

```java
@Component
public class JwtTokenProvider {

    private static final String ROLES_CLAIM = "roles";
    private final SecretKey key;
    private final long validitySeconds;

    public JwtTokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.validitySeconds = properties.accessTokenValiditySeconds();
    }
```

- 생성자에서 비밀키 문자열을 **`SecretKey` 객체로 한 번만** 변환해 보관(`Keys.hmacShaKeyFor`). HMAC-SHA 방식이라 같은 키로 서명하고 같은 키로 검증한다(대칭키).
- ⚠️ 키가 너무 짧으면 jjwt가 예외를 던진다(HS256은 최소 256bit=32바이트). 그래서 기본 secret을 충분히 길게 잡았다.

### 2-1. 발급

```java
public String createToken(String username, Collection<String> roleCodes) {
    Instant now = Instant.now();
    return Jwts.builder()
            .subject(username)                       // sub = 이 토큰의 주인
            .claim(ROLES_CLAIM, List.copyOf(roleCodes))  // roles = [SALES, ...]
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(validitySeconds)))
            .signWith(key)                           // ← 서명! 이게 위조 방지의 전부
            .compact();
}
```

- payload에 **username과 역할 코드 목록**을 담는다. 브리핑 §3-2의 경고대로 **민감정보(비밀번호 등)는 절대 안 넣는다** — payload는 누구나 Base64 디코드로 까볼 수 있다(암호화가 아니라 인코딩).
- 역할을 토큰에 넣었기 때문에, 이후 요청마다 **DB를 안 봐도** 권한 검사가 가능하다(무상태의 이점). 단점: 로그인 후 역할이 바뀌어도 토큰 만료 전까진 옛 역할이 유지된다 — access token을 짧게(1시간) 잡는 이유.

### 2-2. 검증

```java
public boolean validate(String token) {
    try {
        parse(token);
        return true;
    } catch (Exception e) {
        log.debug("JWT 검증 실패: {}", e.getMessage());
        return false;   // ← 예외를 밖으로 안 던지고 false 로 흡수
    }
}

private Claims parse(String token) {
    return Jwts.parser()
            .verifyWith(key)            // 같은 키로 서명 검증
            .build()
            .parseSignedClaims(token)   // 서명 깨졌거나 만료면 여기서 예외
            .getPayload();
}
```

- `parseSignedClaims` 가 **서명 불일치**(위조)와 **만료**를 모두 잡아 예외를 던진다.
- `validate` 가 예외를 `false` 로 바꾸는 이유: 필터에서 "토큰이 유효하면 인증 세팅, 아니면 그냥 통과(=익명)"로 단순 분기하기 위해서. 인증 실패의 최종 응답(401)은 뒤쪽 `AuthenticationEntryPoint`가 책임진다(워크스루 ⑥).

---

## 3. `JwtAuthenticationFilter` — 매 요청, 토큰 → SecurityContext

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "ACCESS_TOKEN";
    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ... {
        String token = resolveToken(request);
        if (token != null && tokenProvider.validate(token)) {
            String username = tokenProvider.getUsername(token);
            List<SimpleGrantedAuthority> authorities = tokenProvider.getRoles(token).stream()
                    .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
                    .toList();
            var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);   // 토큰 없어도 일단 통과 (뒤에서 막음)
    }
```

- **`OncePerRequestFilter`**: 한 요청에 정확히 한 번만 실행됨을 보장하는 Spring 베이스 필터.
- **토큰이 유효할 때만** `SecurityContextHolder` 에 인증 객체를 심는다. 이 순간부터 그 요청은 "인증된 사용자"가 된다.
- 역할 코드 `SALES` 에 **`ROLE_` 접두어**를 붙여 권한으로 만든다 — Spring Security의 `hasRole('SALES')` 규약(워크스루 ②·⑥에서 더). 토큰엔 접두어 없이 `SALES`로 저장하고, 권한으로 변환할 때만 붙인다.
- 비밀번호 자리에 `null` 을 넣는다(`UsernamePasswordAuthenticationToken(username, null, authorities)`). 이미 토큰으로 인증을 마쳤으므로 자격증명은 필요 없다 — "이 토큰을 들고 있다"는 사실 자체가 인증이다.

### 3-1. 토큰을 어디서 찾나 — 헤더 + 쿠키 둘 다

```java
private String resolveToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith(BEARER_PREFIX)) {
        return header.substring(BEARER_PREFIX.length());   // API 클라이언트
    }
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();                  // 브라우저
            }
        }
    }
    return null;
}
```

이 "둘 다 본다"가 설계 결정의 핵심(설계 §7-1):

| 클라이언트 | 토큰 전달 방식 | 이유 |
| --- | --- | --- |
| Swagger UI / 테스트 / 외부 시스템 | `Authorization: Bearer ...` 헤더 | 표준 API 방식 |
| 브라우저 (Thymeleaf 화면) | `ACCESS_TOKEN` HttpOnly 쿠키 | 매 요청 헤더 수동 첨부가 번거로움 + JS 접근 차단(XSS 완화) |

→ 한 필터가 양쪽을 모두 받아주므로, 같은 백엔드를 API로도 화면으로도 쓸 수 있다.

---

## 4. 누적성 — 앞 Phase와의 연결

- **Phase 2의 HTTP 상태 코드 의미** → 토큰 검증 실패가 결국 401로 이어진다(워크스루 ⑥).
- 이 필터가 `SecurityContext` 에 심은 "현재 사용자"는 두 곳에서 다시 쓰인다:
  - **인가**: `@PreAuthorize` 가 이 권한을 본다(워크스루 ②·⑥).
  - **감사 층위①**: `SecurityAuditorAware` 가 이 사용자명을 꺼내 `created_by` 에 박는다(워크스루 ④).

→ 즉 이 필터는 "한 번 인증해서 컨텍스트에 올려두면, 인가와 감사가 그걸 공유한다"는 Phase 6 전체 구조의 입구다.

---

## 5. 한 줄 요약

> JWT는 **서버가 기억하지 않는 서명된 신분증**이다. `JwtTokenProvider`가 비밀키로 발급·검증하고, `JwtAuthenticationFilter`가 매 요청 그 신분증을 까서 `SecurityContext`에 "현재 사용자"를 복원한다. 헤더와 쿠키를 모두 받아 API와 브라우저를 한 백엔드로 처리한다.
