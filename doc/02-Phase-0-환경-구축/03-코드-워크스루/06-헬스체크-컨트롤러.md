# 6/9. `HealthController.java` — REST 진입점

```java
package com.hwlee.erp.common.health;

import javax.sql.DataSource;
import java.sql.Connection;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        try (Connection conn = dataSource.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT 1")) {
            rs.next();
            return ResponseEntity.ok(HealthResponse.up("OK"));
        } catch (Exception e) {
            return ResponseEntity.status(503)
                    .body(HealthResponse.down("FAIL: " + e.getMessage()));
        }
    }
}
```

---

## 🔥 1. `@RestController` vs `@Controller`

| 어노테이션 | 반환값 의미 |
|---|---|
| `@Controller` | 반환값 = **뷰 이름** (`"home"` → `home.html`로 렌더) |
| `@RestController` | 반환값 = **HTTP 응답 본문** (Jackson이 JSON으로 직렬화) |

`@RestController` = `@Controller + @ResponseBody` 합성. **API 서버는 무조건 `@RestController`**.

→ Phase 6에서 Thymeleaf 화면 도입할 때 `@Controller`를 별도 클래스로 추가. API와 화면은 컨트롤러를 섞지 않는다 (책임 분리).

---

## 🔥 2. 라우팅: `@RequestMapping("/api/health")` + `@GetMapping`

```java
@RequestMapping("/api/health")  // 클래스 레벨 경로
public class HealthController {
    @GetMapping                  // 메서드 레벨 경로 (없으면 클래스 경로 그대로)
    public ResponseEntity<HealthResponse> health() { ... }
}
```

→ 최종 매핑: `GET /api/health`

**`/api/` 접두사를 둔 이유** (실무 패턴):

- Phase 6에서 `/login`, `/menu` 같은 화면 URL이 추가될 예정
- API와 화면 URL이 섞이면 Spring Security 규칙이 복잡해짐 → `/api/**`는 JWT 인증, `/**`는 폼 로그인 식으로 분리하기 쉬움

다른 HTTP 메서드용:

- `@PostMapping` → 등록 (Phase 1에서 고객 등록 시 첫 등장)
- `@PutMapping` → 전체 수정
- `@PatchMapping` → 부분 수정
- `@DeleteMapping` → 삭제 (마스터는 Soft Delete라 잘 안 씀)

---

## 🔥 3. 생성자 주입 — Spring 4.3+ 권장 패턴

```java
private final DataSource dataSource;

public HealthController(DataSource dataSource) {
    this.dataSource = dataSource;
}
```

**`@Autowired`가 없는데도 주입이 되는 이유**: 생성자가 하나뿐인 빈은 Spring이 자동으로 생성자 주입.

비교 — 세 가지 주입 방식:

| 방식 | 코드 | 평가 |
|---|---|---|
| 필드 주입 | `@Autowired DataSource ds;` | ❌ 테스트 어려움, `final` 못 씀 |
| Setter 주입 | `@Autowired public void setDs(...)` | ❌ 의존성이 변경 가능 = 가변 |
| **생성자 주입** | `public HealthController(DataSource ds)` | ✅ 불변, 테스트 시 `new HealthController(mock)` |

→ ERP는 서비스 객체가 많고 의존성이 깊어짐. 생성자 주입을 일관되게 쓰면 **순환 의존성**이 컴파일/기동 시점에 즉시 드러남 (필드 주입은 늦게 드러남).

**Lombok `@RequiredArgsConstructor`**를 쓰면 보일러플레이트 제거:

```java
@RestController
@RequiredArgsConstructor   // final 필드만 받는 생성자 자동 생성
@RequestMapping("/api/health")
public class HealthController {
    private final DataSource dataSource;  // 이 한 줄만 두면 됨
}
```

→ Phase 1부터 본격 도입 예정 (지금은 한 줄짜리라 명시).

---

## 🔥 4. `DataSource`를 직접 받는다 — 평소엔 안 하는 일

ERP에서 보통은 `JdbcTemplate`이나 `JpaRepository`를 받는다. **여기서 `DataSource`를 직접 받은 이유**:

- 헬스체크는 "**DB 연결 자체가 살아있는가**"를 확인 → JPA/Hibernate를 거치면 그것들이 캐시한 결과를 줄 수 있음
- `DataSource` → 커넥션 풀(HikariCP)에서 직접 한 커넥션 빌려와 SQL 실행 → 가장 낮은 레벨에서 검증

→ "왜 헬스체크는 JPA 위에서 하지 않는가"의 답.

---

## 🔥 5. try-with-resources — 커넥션 누수 방지

```java
try (Connection conn = dataSource.getConnection();
     var stmt = conn.createStatement();
     var rs = stmt.executeQuery("SELECT 1")) {
    rs.next();
    return ResponseEntity.ok(HealthResponse.up("OK"));
}
```

**3개 자원이 한 try에 묶여 있다**:

- `Connection` → 커넥션 풀에 반납
- `Statement` → JDBC 자원 해제
- `ResultSet` → 커서 닫기

세미콜론으로 구분. **닫히는 순서는 선언의 역순** (`rs` → `stmt` → `conn`). 익셉션이 나도 무조건 닫힘.

**커넥션 누수가 일어나면**:

- 기본 풀 크기 10 (Hikari 기본값)
- 10번 누수 → 11번째 요청부터 영구 hang → 전체 시스템 마비

→ "**기간계의 가장 흔한 장애 = 커넥션 풀 고갈**". try-with-resources는 반드시 습관화.

`var`를 쓴 이유: 타입이 명확하고 한 줄짜리라 가독성 ↑. Java 10+ 기능.

---

## 🔥 6. `SELECT 1` — JDBC 헬스체크의 표준 관용구

```sql
SELECT 1
```

- 테이블 의존성 없음 (어떤 권한도 필요 없음)
- 가장 가벼운 쿼리
- DB 종류와 무관 (MySQL, PostgreSQL, Oracle 다 됨)

**왜 `SELECT NOW()`가 아닌가?**

- DB가 살아있긴 한데 시간대 설정이 깨졌으면 `SELECT NOW()`는 통과해도 데이터는 망가짐 → 헬스체크의 책임 범위 밖
- 헬스체크는 "**최소한의 연결성**"만 확인. 더 깊은 검증은 별도 진단 API.

`rs.next()`를 한 번 호출하는 것도 의미가 있다: ResultSet을 실제로 읽어야 일부 JDBC 드라이버는 쿼리가 완료됐다고 본다.

---

## 🔥 7. `ResponseEntity` — HTTP 상태 코드 명시

```java
return ResponseEntity.ok(HealthResponse.up("OK"));         // 200 OK
return ResponseEntity.status(503).body(HealthResponse.down(...));  // 503 Service Unavailable
```

만약 그냥 `return new HealthResponse(...)`로 끝낸다면 항상 200 OK가 나감 → 모니터링 도구가 "DB 죽었는데 헬스체크는 200"으로 오해.

**503 (Service Unavailable)을 고른 이유**:

- 500 = 서버 자체의 버그
- 503 = "서버는 살아있지만 **의존하는 외부 자원이 죽었음**" → 의미상 정확
- 로드밸런서/k8s probe는 503을 보면 그 인스턴스를 트래픽에서 제외시킴

---

## 🔥 8. 예외 메시지를 응답에 노출 — 학습용 한정

```java
return ResponseEntity.status(503)
    .body(HealthResponse.down("FAIL: " + e.getMessage()));
```

**운영에서는 ❌ 안 좋은 패턴**: 예외 메시지에 내부 정보(DB 호스트, SQL 일부 등) 노출 → 보안 사고.

학습 단계라 일부러 메시지를 그대로 노출. Phase 6 (Security/감사 로그)에서 **`@ControllerAdvice` 글로벌 예외 처리기**를 도입하면서 "외부 노출 메시지 vs 내부 로그 메시지"를 분리할 예정.

---

## 한 요청이 처리되는 시퀀스 (이 컨트롤러 기준)

```
브라우저: GET /api/health
   ↓
Tomcat (port 8080)
   ↓
DispatcherServlet ← Spring MVC의 진입점
   ↓
@RequestMapping 매핑 테이블 검색 → HealthController#health() 찾음
   ↓
HealthController.health() 실행
   ↓ DataSource.getConnection() → HikariCP 풀에서 빌림
   ↓ SELECT 1 실행
   ↓ Connection 반납 (try-with-resources)
   ↓ HealthResponse 반환
   ↓
Jackson이 HealthResponse → JSON 직렬화
   ↓
HTTP/1.1 200 OK
Content-Type: application/json
{"status":"UP","db":"OK","timestamp":"..."}
```

→ Phase 1에서 "고객 등록 API"가 추가되면 이 시퀀스의 중간에 **`@Valid` (검증) → `@Transactional` (트랜잭션 시작) → JpaRepository → Hibernate → SQL** 가 끼어든다. 흐름의 골격은 같음.
