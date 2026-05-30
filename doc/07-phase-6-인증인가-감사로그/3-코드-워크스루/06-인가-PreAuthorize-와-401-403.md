# Phase 6 코드 워크스루 ⑥ — 인가(@PreAuthorize)와 401/403 예외 처리

> 대상 파일
> - 18개 컨트롤러의 클래스/메서드 레벨 `@PreAuthorize`
> - `security/config/RestAuthenticationEntryPoint.java` (401)
> - `security/config/RestAccessDeniedHandler.java` (403)
> - `common/error/GlobalExceptionHandler.java` (401/403 핸들러 추가)
>
> 이 글의 목표: RBAC 인가를 **어디에 어떻게** 거는지, 그리고 인증 실패(401)와 인가 실패(403)가 **같은 에러 포맷**으로 응답되게 하는 법을 본다. 브리핑 §2(401 vs 403)·§4(RBAC).

---

## 0. 설계 정정 한 가지 — URL 패턴이 아니라 메서드 보안

브리핑 §3-3은 경로를 `/api/sd/sales-orders`처럼 가정했지만, **실제 컨트롤러 경로엔 모듈 prefix가 없다**:

| 모듈 | 실제 경로 |
| --- | --- |
| SD | `/api/sales-orders`, `/api/quotations`, `/api/deliveries`, `/api/invoices` |
| MM | `/api/goods-issues`, `/api/goods-receipts`, `/api/warehouses`, `/api/stocks`, `/api/stock-movements` |
| FI | `/api/journal-entries`, `/api/payments`, `/api/accounts` |
| master | `/api/customers`, `/api/items`, `/api/vendors`, `/api/departments`, `/api/employees` |

→ `/api/sd/**` 같은 **URL 패턴 인가가 불가능**(경로에 모듈명이 없음). 그래서 설계 §4-1에서 **클래스 단위 `@PreAuthorize`**(메서드 보안)로 방향을 틀었다. 모듈 소속이 코드 위치(`sd/`, `fi/`)로 자명하므로, 컨트롤러 클래스에 한 줄 거는 게 가장 명확하다.

(이 결정이 워크스루 ③의 `@EnableMethodSecurity`가 필요한 이유다.)

---

## 1. 모듈별 인가 — 클래스 레벨

각 컨트롤러 클래스 위에 한 줄:

```java
// SD 컨트롤러 4개
@PreAuthorize("hasAnyRole('SALES','ADMIN')")     // 영업팀 + 관리자

// MM 컨트롤러 5개
@PreAuthorize("hasAnyRole('PURCHASING','ADMIN')")

// FI 컨트롤러 3개
@PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
```

- **클래스에 걸면 그 안의 모든 핸들러 메서드에 적용**된다. SD 컨트롤러의 모든 엔드포인트가 SALES/ADMIN 전용이 된다.
- `hasAnyRole('SALES','ADMIN')`은 `ROLE_SALES` 또는 `ROLE_ADMIN` 권한을 요구한다 — 워크스루 ②에서 역할 코드에 `ROLE_` 접두어를 붙인 것과 짝.
- **ADMIN을 모든 모듈에 포함**: 관리자는 전 모듈 접근(브리핑 §4-1 역할표).

---

## 2. master — 조회는 전 역할, 변경은 ADMIN

마스터 데이터는 "조회는 누구나, 변경은 관리자만"이 자연스럽다. 클래스 레벨은 넓게, 변경 메서드만 좁힌다:

```java
@RestController
@RequestMapping("/api/customers")
// 클래스: 조회 포함 전 업무 역할 허용
@PreAuthorize("hasAnyRole('SALES','PURCHASING','FINANCE','ADMIN')")
public class CustomerController {

    @PreAuthorize("hasRole('ADMIN')")          // ← 메서드: 변경은 ADMIN만 (좁힘)
    @PostMapping
    public ... create(...) { ... }

    @GetMapping                                 // ← 조회는 클래스 레벨 그대로 (전 역할)
    public ... search(...) { ... }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ... update(...) { ... }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ... delete(...) { ... }
}
```

- **메서드 레벨 `@PreAuthorize`가 클래스 레벨을 오버라이드**한다. create/update/delete는 ADMIN으로 좁혀지고, GET 계열은 클래스 레벨(전 역할)이 유지된다.
- 5개 master 컨트롤러(customer/item/vendor/department/employee) 모두 같은 패턴 — 클래스 1개 + 메서드 3개(POST/PUT/DELETE).

> 💡 "SALES가 고객 목록은 보지만 고객을 못 만든다"가 코드로 표현된다. 영업이 거래하려면 고객 정보는 봐야 하지만, 마스터 등록은 관리자 통제 하에 둔다 — 내부통제(브리핑 §1).

---

## 3. 401 vs 403 — 두 실패는 완전히 다르다

브리핑 §2의 핵심:

| 실패 | 의미 | 상태 | 누가 처리 |
| --- | --- | --- | --- |
| **인증 실패** | "너 누군지 모르겠다"(미인증/토큰 무효) | **401** | `RestAuthenticationEntryPoint` |
| **인가 실패** | "누군진 알겠는데 권한 없다" | **403** | `RestAccessDeniedHandler` |

문제: 이 두 실패는 **Security 필터 단계**에서 일어나 컨트롤러에 도달하기 전이다. 그래서 `@RestControllerAdvice`(`GlobalExceptionHandler`)가 못 잡는다 → 별도 핸들러가 필요하다.

### 3-1. EntryPoint — 401

```java
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (isApiRequest(request)) {     // /api/** → JSON
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNAUTHORIZED, "인증이 필요합니다. 로그인 후 다시 시도해 주세요.");
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), pd);
        } else {                          // 브라우저 화면 → 로그인 페이지로
            response.sendRedirect("/login");
        }
    }
}
```

- **API 요청(`/api/**`)이면 ProblemDetail JSON**, 화면 요청이면 `/login`으로 리다이렉트. 한 백엔드가 API와 브라우저를 동시에 섬기는 설계(워크스루 ①)의 마무리.
- **기존 에러 포맷과 동일한 `ProblemDetail`**을 손으로 써준다 — Phase 2~5에서 쓰던 RFC 9457 포맷과 일관성 유지.

### 3-2. AccessDeniedHandler — 403

```java
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "이 작업을 수행할 권한이 없습니다.");
        response.setStatus(403);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), pd);
    }
}
```

이 둘은 `SecurityConfig`의 `exceptionHandling(...)`에 등록된다(워크스루 ③ §1-2).

### 3-3. GlobalExceptionHandler에도 보강

메서드 보안(`@PreAuthorize`)이 컨트롤러 진입 후 던지는 `AccessDeniedException`, 그리고 `AuthService` 로그인 검증이 던지는 `AuthenticationException`은 `@RestControllerAdvice`로도 잡히므로, 일관성을 위해 같은 포맷으로 추가했다:

```java
@ExceptionHandler(AuthenticationException.class)   // 로그인 실패 등 → 401
public ProblemDetail handleAuthentication(AuthenticationException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
            "이메일 또는 비밀번호가 올바르지 않습니다.");
}

@ExceptionHandler(AccessDeniedException.class)      // @PreAuthorize 거부 → 403
public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
            "이 작업을 수행할 권한이 없습니다.");
}
```

→ 어느 경로로 실패하든 클라이언트는 **동일한 ProblemDetail JSON**을 받는다.

---

## 4. 테스트로 본 인가 — `AuthorizationTest`

"영업팀은 SD만, 재무팀은 FI만"이 코드로 검증된다:

```java
@Test @WithMockUser(roles = "SALES")
void sales_cannot_access_finance_module_returns_403() throws Exception {
    mockMvc.perform(get("/api/journal-entries")).andExpect(status().isForbidden());   // SALES → FI: 403
}

@Test @WithMockUser(roles = "FINANCE")
void finance_can_access_finance_module() throws Exception {
    mockMvc.perform(get("/api/journal-entries")).andExpect(status().isOk());          // FINANCE → FI: 200
}

@Test @WithMockUser(roles = "SALES")
void sales_can_read_master_but_cannot_write_returns_403() throws Exception {
    mockMvc.perform(get("/api/customers")).andExpect(status().isOk());                // 조회 OK
    mockMvc.perform(delete("/api/customers/1")).andExpect(status().isForbidden());    // 변경 403
}
```

그리고 미인증 401(`AuthControllerTest`):

```java
mockMvc.perform(post("/api/customers").content("{}"))
        .andExpect(status().isUnauthorized());   // 토큰 없음 → 401
```

→ 401(인증)과 403(인가)이 정확히 구분되어 응답된다.

---

## 5. 누적성

```
Phase 2: HTTP 상태 코드 의미 (400/404/409/422)
   └─→ Phase 6: 401(인증)·403(인가) 추가 — 의미 차이의 연장
Phase 5: "내부통제 — 누가 전표를 확정하나"
   └─→ Phase 6: FINANCE만 FI 접근 — 인가로 강제
```

---

## 6. 자기 점검

- [ ] URL 패턴 인가 대신 클래스 단위 `@PreAuthorize`를 택한 이유는?
- [ ] 클래스 레벨과 메서드 레벨 `@PreAuthorize`가 함께 있으면 어느 쪽이 적용되는가?
- [ ] 401과 403의 의미 차이, 그리고 각각 어떤 컴포넌트가 처리하는가?
- [ ] Security 필터 단계 예외를 `GlobalExceptionHandler`가 못 잡는 이유는?
- [ ] `hasRole('FINANCE')`와 DB의 `role.code='FINANCE'`(접두어 없음)가 맞아떨어지는 메커니즘은?

---

## 7. 한 줄 요약

> 인가는 URL이 아니라 **클래스/메서드 단위 `@PreAuthorize`**로 건다(경로에 모듈명이 없어서). 인증 실패(401)와 인가 실패(403)는 필터 단계에서 일어나므로 전용 EntryPoint/Handler로 처리하되, 기존과 같은 ProblemDetail 포맷으로 일관되게 응답한다.
