# 7/9. `HealthResponse.java` — 응답 DTO

```java
package com.hwlee.erp.common.health;

import java.time.OffsetDateTime;

public record HealthResponse(
        String status,
        String db,
        OffsetDateTime timestamp
) {
    public static HealthResponse up(String dbStatus) {
        return new HealthResponse("UP", dbStatus, OffsetDateTime.now());
    }

    public static HealthResponse down(String dbStatus) {
        return new HealthResponse("DOWN", dbStatus, OffsetDateTime.now());
    }
}
```

18줄짜리지만 자바 21의 핵심 모던 기능 두 개가 들어 있음.

---

## 🔥 1. `record` — Java 14+에서 도입된 불변 데이터 클래스

이 한 줄이...

```java
public record HealthResponse(String status, String db, OffsetDateTime timestamp) { }
```

...자바 표준 클래스로 펼치면 이렇다:

```java
public final class HealthResponse {
    private final String status;
    private final String db;
    private final OffsetDateTime timestamp;

    public HealthResponse(String status, String db, OffsetDateTime timestamp) {
        this.status = status;
        this.db = db;
        this.timestamp = timestamp;
    }

    public String status() { return status; }
    public String db() { return db; }
    public OffsetDateTime timestamp() { return timestamp; }

    @Override public boolean equals(Object o) { /* 모든 필드 비교 */ }
    @Override public int hashCode() { /* 모든 필드 해시 */ }
    @Override public String toString() { /* HealthResponse[status=..., db=..., ...] */ }
}
```

→ **30줄이 1줄로 줄어듦**. 모든 필드가 자동 `final` = 불변.

**Lombok `@Value`와의 차이**:

| | record | Lombok `@Value` |
|---|---|---|
| 표준 | Java 표준 문법 | 외부 라이브러리 |
| getter | `name()` (괄호 있음) | `getName()` |
| 상속 | `final` 강제, 상속 불가 | 가능 |
| 컴파일 의존성 | 없음 | annotation processor 필요 |

→ Lombok도 쓰지만, **DTO처럼 데이터만 담는 객체는 `record`가 정공법** (Java 표준 + IDE 지원 더 좋음).

---

## 🔥 2. `record`를 어디에 쓰고 어디에 안 쓰나 — ERP 학습 포인트

**✅ `record`가 적합**:

- DTO (요청/응답): `CustomerCreateRequest`, `CustomerResponse`
- 검색 조건: `OrderSearchCondition`
- 값 객체(Value Object): `Money`, `Address` — 같은 값이면 같은 인스턴스

**❌ `record`가 부적합**:

- **JPA 엔티티(`@Entity`)** — JPA는 기본 생성자 + setter (또는 필드 변경)를 요구. record는 불변이라 안 맞음
- 비즈니스 로직이 많은 도메인 객체 — 메서드를 두긴 하지만 가변 상태가 필요한 경우

→ 이 구분이 Phase 1에서 매우 중요해진다:

```java
// Entity (가변)
@Entity
public class Customer { ... }  // record 안 됨

// DTO (불변)
public record CustomerCreateRequest(String name, String bizNo) { }  // record OK
public record CustomerResponse(Long id, String name, String code) { }  // record OK
```

---

## 🔥 3. 정적 팩토리 메서드 (`up`, `down`)

```java
public static HealthResponse up(String dbStatus) {
    return new HealthResponse("UP", dbStatus, OffsetDateTime.now());
}
```

생성자 `new HealthResponse("UP", "OK", OffsetDateTime.now())`로도 만들 수 있는데, 굳이 `HealthResponse.up("OK")`를 둔 이유:

**1) 이름이 의미를 전달한다**

- `new HealthResponse("UP", ...)` ← "UP" 문자열을 직접 넘김 = 오타 위험
- `HealthResponse.up("OK")` ← 메서드 이름으로 의도를 표현

**2) 호출자에서 timestamp를 신경 쓸 필요 없음**

- 매번 `OffsetDateTime.now()`를 부르는 보일러플레이트 제거
- 시간 생성 로직이 한 곳(메서드 내부)에 모임 → 나중에 "시간을 Clock 빈으로 주입받자"는 테스트 가능성 개선 시 한 곳만 고치면 됨

**3) 상태 종류가 제한적임을 코드로 표현**

- `up` / `down` 두 정적 팩토리만 노출 → "**HealthResponse가 가질 수 있는 상태는 이 둘뿐**"이라는 도메인 규칙이 API로 드러남
- 외부에서 `new HealthResponse("HALF_UP", ...)` 같은 이상한 상태를 만드는 걸 막진 못하지만, **권장 경로를 명확히 함**
- 더 강하게 막으려면 → 생성자를 `private`로 만들고 정적 팩토리만 노출 (record는 생성자 접근 제어자 변경 가능)

→ ERP에서 `record`로 DTO를 만들 때 **정적 팩토리 메서드를 함께 두는 패턴**이 자주 등장한다. 예:

```java
public record OrderResponse(Long id, String status, BigDecimal total) {
    public static OrderResponse from(Order entity) {  // 엔티티 → DTO 변환
        return new OrderResponse(entity.getId(), entity.getStatus().name(), entity.getTotal());
    }
}
```

---

## 🔥 4. `OffsetDateTime` — 시간 타입 선택

```java
OffsetDateTime timestamp
```

자바의 시간 타입은 여러 종류:

| 타입 | 표현 | ERP에서 |
|---|---|---|
| `LocalDate` | 2026-05-11 (시간 없음) | 생일, 입사일 |
| `LocalDateTime` | 2026-05-11T14:30 (시간대 없음) | ❌ 위험 — 어느 시간대인지 모름 |
| **`OffsetDateTime`** | 2026-05-11T14:30+09:00 (시간대 오프셋 포함) | ✅ API 응답, DB 저장 |
| `ZonedDateTime` | 2026-05-11T14:30+09:00 Asia/Seoul (시간대 ID 포함) | DST 처리 필요 시 |
| `Instant` | epoch milliseconds | 로그/이벤트 |

**왜 `OffsetDateTime`인가?**

- JSON 직렬화 결과: `"2026-05-11T14:30:00+09:00"` (ISO 8601 표준)
- 클라이언트가 어느 시간대에서 보든 모호함 없음
- MySQL의 `DATETIME` 컬럼과도 호환

**❌ `LocalDateTime` 함정** (실무 사고 단골):

- "2026-05-11 14:30" — 이게 한국 시간인지 UTC인지 코드에 안 적혀 있음
- 다국적 ERP에서 출하 시각을 `LocalDateTime`으로 저장 → 다른 시간대 사용자가 봤을 때 9시간 어긋남
- **글로벌 ERP는 항상 `OffsetDateTime` 또는 UTC `Instant`**

→ Phase 5 (FI 회계) 에서 회계 기준일/거래일을 다룰 때 이 구분이 핵심이 된다.

---

## 🔥 5. Jackson이 record를 JSON으로 바꾸는 방식

`@RestController`가 `HealthResponse`를 반환하면 Jackson이 자동으로 직렬화:

```json
{
  "status": "UP",
  "db": "OK",
  "timestamp": "2026-05-11T20:34:00+09:00"
}
```

- 필드명 = JSON 키 (`status`, `db`, `timestamp`)
- record의 접근자 메서드(`status()`)를 호출해 값을 가져옴
- `OffsetDateTime`은 `jackson-datatype-jsr310` 모듈이 ISO 8601로 변환 (Spring Boot가 자동 등록)

→ 옛날 자바엔 `Date` 직렬화 결과가 timestamp(epoch ms)였다 → 가독성 ❌ + 시간대 정보 없음. Java 8 time API + Jackson JSR310 모듈로 깨끗하게 해결됨.

---

## 패키지 위치: `common/health/`

```
com/hwlee/erp/
  ├─ HwleeErpApplication.java
  └─ common/                 ← 모든 모듈이 공통으로 쓰는 것
      └─ health/             ← 헬스체크 한 묶음 (Controller + Response)
          ├─ HealthController.java
          └─ HealthResponse.java
```

**`common`에 둔 이유**: 헬스체크는 SD/MM/FI 어디에도 속하지 않는 시스템 공통 기능.

Phase 1에서 `common/` 아래 추가될 것들:

- `common/audit/` — JPA Auditing 설정 (`@EnableJpaAuditing`, `AuditingEntityListener`)
- `common/exception/` — `@ControllerAdvice` 전역 예외 처리
- `common/code/` — 코드 체계 생성기 (`CUST-2026-0001`)
