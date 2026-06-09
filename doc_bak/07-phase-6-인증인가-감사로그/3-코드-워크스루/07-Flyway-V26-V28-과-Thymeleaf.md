# Phase 6 코드 워크스루 ⑦ — 마이그레이션 V26~V28과 Thymeleaf 화면

> 대상 파일
> - `db/migration/V26__create_auth_user_role.sql` / `V27__create_audit_log.sql` / `V28__seed_auth.sql`
> - `security/web/PageController.java` / `security/admin/AdminViewController.java` / `AppUserAdminService.java`
> - `templates/login.html` / `menu.html` / `admin/users.html` / `admin/roles.html`
>
> 이 글의 목표: 스키마(테이블)와 시드(부서 기반 역할 부여), 그리고 최소 Thymeleaf 화면이 어떻게 맞물리는지 본다. 브리핑 §7(화면)·§8(만들 것).

---

## 1. V26 — 인증/인가 5테이블

```sql
-- app_user: 로그인 계정 (Employee 1:1)
CREATE TABLE app_user (
    id BIGINT AUTO_INCREMENT, employee_id BIGINT NOT NULL,
    username VARCHAR(200) NOT NULL, password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE, account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at/by, updated_at/by ...,
    UNIQUE KEY uk_app_user_employee (employee_id),     -- 1:1 강제
    UNIQUE KEY uk_app_user_username (username),
    CONSTRAINT fk_app_user_employee FOREIGN KEY (employee_id) REFERENCES employee(id));

CREATE TABLE role (id, code UNIQUE, name, created/updated ...);
CREATE TABLE permission (id, code UNIQUE, name, created/updated ...);

CREATE TABLE user_role (         -- N:M
    user_id, role_id, PRIMARY KEY (user_id, role_id),
    FK user_role_user → app_user, FK user_role_role → role);

CREATE TABLE role_permission (   -- N:M
    role_id, permission_id, PRIMARY KEY (role_id, permission_id), ...);
```

짚을 점:

- **`employee_id`에 UNIQUE** = 1:1 관계를 DB가 강제(한 직원당 계정 하나). 엔티티의 `@OneToOne`(워크스루 ②)과 짝.
- **`role`/`permission`은 created_by 등 4컬럼을 가짐** = `BaseEntity` 상속(워크스루 ②). 단 `code`만 있고 채번 코드(`BaseEntityWithCode`)는 아님.
- **조인 테이블의 복합 PK** `(user_id, role_id)` = 같은 역할을 두 번 부여하는 중복을 DB가 막는다.
- FK는 전부 명시 — 끊긴 참조(존재하지 않는 role_id 등)를 차단.

---

## 2. V27 — audit_log

```sql
CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT,
    entity_type VARCHAR(100) NOT NULL,
    entity_id   BIGINT       NOT NULL,
    action      VARCHAR(10)  NOT NULL,        -- INSERT/UPDATE/DELETE
    changed_by  VARCHAR(64)  NOT NULL,
    changed_at  DATETIME     NOT NULL,
    changes     LONGTEXT,                      -- JSON 문자열 스냅샷
    PRIMARY KEY (id),
    KEY idx_audit_log_entity (entity_type, entity_id),    -- "이 엔티티의 이력" 조회용
    KEY idx_audit_log_changed_at (changed_at));
```

- **`created_by` 류 4컬럼이 없다** — `AuditLog`는 `BaseEntity` 비상속(워크스루 ⑤). 자기 감사 방지 + 의미 없는 컬럼 제거.
- **`changes`는 `LONGTEXT`** — 원래 `JSON` 타입을 고려했으나, 우리가 콜백에서 직접 직렬화한 문자열을 그대로 저장하므로 단순 텍스트면 충분(MySQL `JSON`은 검증/함수 이점이 있지만 학습 범위엔 과함).
- **복합 인덱스 `(entity_type, entity_id)`** — `AuditLogController`의 "Customer #42 이력 조회"가 이 인덱스를 탄다.

---

## 3. V28 — 시드: 부서 기반 역할 부여 ⭐

확정 ③("부서 기반 시드 + 수동 조정")이 SQL로 구현되는 곳.

### 3-1. 역할·권한·매핑

```sql
INSERT INTO role (code, name, ...) VALUES
    ('SALES','영업'), ('PURCHASING','구매'), ('FINANCE','재무'), ('ADMIN','관리자');

INSERT INTO permission (code, name, ...) VALUES
    ('SD_READ',...), ('SD_WRITE',...), ('FI_POST',...), ('MASTER_READ',...), ...;

-- 역할에 권한 매핑 (code 로 join)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'FINANCE' AND p.code IN ('FI_READ','FI_WRITE','FI_POST','MASTER_READ');
-- ADMIN 은 전 권한
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p WHERE r.code = 'ADMIN';
```

- **id를 하드코딩하지 않고 `code`로 join** — auto_increment id는 환경마다 다를 수 있으니, 의미 있는 코드로 매핑(Phase 5 account 시드와 같은 방식).

### 3-2. 직원 → app_user (BCrypt 해시)

```sql
INSERT INTO app_user (employee_id, username, password_hash, enabled, account_locked, ...)
SELECT e.id, e.email, '$2y$10$fiDYm50/gYzbfsId2MFJqOYpNXfQwoGAX79657DhyryCMTzgbPRoO',
       TRUE, FALSE, ...
  FROM employee e
 WHERE e.email IN ('kim@hwlee-erp.example', 'lee@...', 'park@...', 'admin@...');
```

- **평문이 아니라 BCrypt 해시를 박는다**(브리핑 §3-4 절대 원칙). 이 해시는 `pass1234`를 BCrypt로 인코딩한 값. `$2y$`는 BCrypt 변종이고 Spring의 `BCryptPasswordEncoder`가 검증 가능.
- 기존 직원(V8 시드의 kim/lee/park) + 새로 만든 admin을 계정으로 전환.

### 3-3. 부서 기반 user_role — 핵심

```sql
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
  FROM app_user u
  JOIN employee e   ON e.id = u.employee_id
  JOIN department d ON d.id = e.department_id
  JOIN role r ON r.code = CASE d.code
      WHEN 'DEPT-SALES'    THEN 'SALES'
      WHEN 'DEPT-PURCHASE' THEN 'PURCHASING'
      WHEN 'DEPT-FINANCE'  THEN 'FINANCE'
  END
 WHERE d.code IN ('DEPT-SALES', 'DEPT-PURCHASE', 'DEPT-FINANCE');
```

- **직원의 부서(department)를 보고 역할을 자동 결정** — 영업팀(DEPT-SALES) → SALES 역할. Phase 1의 부서 트리가 여기서 다시 일한다(브리핑 §5의 "부서가 역할을 결정").
- `park`(생산팀, DEPT-PRODUCTION)는 매핑에 없으므로 **역할이 안 붙는다** → 로그인은 되지만 어느 모듈도 못 들어가는 403 시연용 계정.
- `admin`은 별도로 ADMIN 역할 명시 부여.

→ 시드 결과(모두 `pass1234`):

| 계정 | 역할 | 시연 의미 |
| --- | --- | --- |
| `kim@hwlee-erp.example` | SALES | SD 접근 O, FI 접근 403 |
| `lee@hwlee-erp.example` | FINANCE | FI 접근 O |
| `park@hwlee-erp.example` | (없음) | 인증 O, 모든 모듈 403 |
| `admin@hwlee-erp.example` | ADMIN | 전 모듈 + 관리 화면 |

> 💡 "부서 기반 시드 + 수동 조정"의 의미: 시드는 부서로 기본값을 깔고, 이후 변화(팀장에게 ADMIN 추가 등)는 관리자가 화면에서 손본다(§5). 자동과 수동의 절충.

---

## 4. 화면 진입점 — `PageController`

```java
@Controller
public class PageController {
    @GetMapping("/login") public String login() { return "login"; }   // permitAll
    @GetMapping("/")      public String menu()  { return "menu"; }    // 인증 필요
}
```

- `@Controller`(≠`@RestController`) — 뷰 이름(`login`/`menu`)을 반환하면 Thymeleaf가 `templates/login.html`/`menu.html`을 렌더.
- `/login`은 `PUBLIC_PATHS`(워크스루 ③), `/`는 인증 필요 → 미인증이면 EntryPoint가 `/login`으로 리다이렉트(워크스루 ⑥).

### 4-1. 로그인 화면 — 토큰을 쿠키로 받는다

`login.html`의 핵심 JS:

```javascript
const resp = await fetch('/api/auth/login', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({ username, password })
});
if (resp.ok) {
    // 서버가 HttpOnly 쿠키(ACCESS_TOKEN)를 Set-Cookie 로 내려줌 → 이후 페이지 요청에 자동 첨부
    window.location.href = '/';
}
```

- 폼이 `/api/auth/login`(워크스루 ③)을 호출하면, 응답의 `Set-Cookie`로 HttpOnly 쿠키가 자동 저장된다. JS는 토큰을 만질 필요도 없다(HttpOnly라 읽지도 못함).
- 이후 `/`로 이동하면 브라우저가 쿠키를 자동 첨부 → `JwtAuthenticationFilter`가 쿠키에서 토큰을 꺼내 인증(워크스루 ①의 "쿠키도 본다").

### 4-2. 메뉴 — 역할별 노출 (`sec:authorize`)

`menu.html`은 Thymeleaf Security 확장(`thymeleaf-extras-springsecurity6`)으로 역할에 따라 메뉴를 보여준다:

```html
<div sec:authorize="hasAnyRole('SALES','ADMIN')">  ... 영업(SD) 카드 ... </div>
<div sec:authorize="hasAnyRole('FINANCE','ADMIN')"> ... 회계(FI) 카드 ... </div>
<div sec:authorize="hasRole('ADMIN')">             ... 관리자 카드 ... </div>
```

- **백엔드 인가(`@PreAuthorize`)와 화면 노출(`sec:authorize`)은 별개의 두 겹**이다. 화면에서 메뉴를 숨겨도 그건 UX일 뿐, 진짜 차단은 백엔드가 한다(URL 직접 입력 시 403). 화면 숨김은 "보이지 않게", 백엔드는 "할 수 없게".

---

## 5. 관리자 화면 — 역할 수동 조정

`AdminViewController` + `AppUserAdminService`로 "사용자 ↔ 역할" 매핑을 화면에서 바꾼다(확정 ③의 "수동 조정"):

```java
@Controller @PreAuthorize("hasRole('ADMIN')")
public class AdminViewController {
    @GetMapping("/admin/users")            // 사용자 목록 + 역할 체크박스
    @PostMapping("/admin/users/{id}/roles") // 체크된 역할로 교체
    @GetMapping("/admin/roles")            // 역할-권한 조회
}
```

```java
@Transactional
public void replaceRoles(Long userId, List<Long> roleIds) {
    AppUser user = appUserRepository.findById(userId).orElseThrow(...);
    List<Role> roles = roleIds == null ? List.of() : roleRepository.findAllById(roleIds);
    user.getRoles().clear();           // 기존 전부 회수
    roles.forEach(user::grantRole);    // 선택분 부여 (워크스루 ② 의 엔티티 메서드)
}
```

- **컨트롤러 자체가 `@PreAuthorize("hasRole('ADMIN')")`** — 관리 화면은 관리자만.
- "기존 clear 후 선택분 grant"는 체크박스 UI(전체 상태를 매번 통째로 제출)와 잘 맞는 단순·명확한 방식.
- `users.html`은 각 사용자 행에 역할 체크박스 + [저장] 버튼, `roles.html`은 역할별 권한 배지를 보여준다.

---

## 6. 자기 점검

- [ ] V28에서 역할 매핑을 id 대신 `code`로 join하는 이유는?
- [ ] `password_hash`에 평문이 아니라 BCrypt 해시를 박는 이유는?
- [ ] `park` 계정이 "로그인은 되지만 모든 모듈 403"인 이유는? (시드 어디서 갈렸나)
- [ ] 화면의 `sec:authorize` 숨김과 백엔드 `@PreAuthorize` 차단의 관계는?
- [ ] 브라우저가 매 요청에 토큰을 자동으로 싣는 메커니즘은? (힌트: HttpOnly 쿠키)

---

## 7. 한 줄 요약

> V26/V27이 인증·감사 테이블을 만들고, V28이 **직원의 부서를 보고 역할을 자동 시드**(부서 기반)한 뒤 BCrypt 해시로 계정을 깐다. Thymeleaf는 로그인(쿠키 수령)·메뉴(`sec:authorize` 역할별 노출)·관리자(역할 수동 조정) 최소 화면을 제공하되, 진짜 차단은 언제나 백엔드 `@PreAuthorize`가 한다.
