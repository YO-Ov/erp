# 3/9. Auditing & 공통 인프라 — `JpaAuditingConfig` / `ClockConfig` / `GlobalExceptionHandler`

> 마스터 도메인 코드의 뒤를 받쳐주는 횡단 관심사 3종. 작지만 모든 엔드포인트의 동작에 영향을 준다.

대상 파일:

```
hwlee-erp/src/main/java/com/hwlee/erp/common/
├─ audit/JpaAuditingConfig.java        (Auditing 활성화)
├─ time/ClockConfig.java               (Clock 빈)
└─ error/GlobalExceptionHandler.java   (예외 → ProblemDetail 변환)
```

---

## 🔥 `JpaAuditingConfig` — `@CreatedBy` 가 자동으로 채워지는 비결

```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    static final String SYSTEM_USER = "system";

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(SYSTEM_USER);
    }
}
```

### `@EnableJpaAuditing` 이 켜는 것

01편의 `BaseEntity` 를 다시 보자:

```java
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate    private LocalDateTime createdAt;
    @CreatedBy      private String createdBy;
    @LastModifiedDate private LocalDateTime updatedAt;
    @LastModifiedBy   private String updatedBy;
}
```

이 어노테이션 4개는 **`@EnableJpaAuditing` 가 어딘가에 켜져 있어야만** 의미가 있다. 안 켜져 있으면 INSERT 직전에 채워주는 리스너가 동작하지 않아서 NOT NULL 컬럼인데 null 인 채로 들어가 → 제약 위반.

→ 이 한 줄이 모든 마스터의 4컬럼을 자동으로 채워주는 스위치다.

### `auditorAwareRef = "auditorProvider"` 의 의미

`@CreatedBy` / `@LastModifiedBy` 에 무슨 값을 박을지를 Spring 이 알려면 누가 "현재 사용자" 를 알려줘야 한다. 그 책임이 `AuditorAware<T>` 빈이다.

```java
@Bean
public AuditorAware<String> auditorProvider() {
    return () -> Optional.of(SYSTEM_USER);
}
```

Phase 1은 인증이 없으니 **무조건 "system"** 을 돌려준다. INSERT/UPDATE 가 일어날 때마다 Spring 이 이 람다를 호출해서 "system" 을 받아 `created_by` / `updated_by` 에 박는다.

### Phase 6 의 교체 지점

설계서 §6.2 에서 미리 예고한 부분. Phase 6 에서 Spring Security 가 들어오면 이 람다 본문이 이렇게 바뀐다:

```java
return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .filter(Authentication::isAuthenticated)
        .map(Authentication::getName);
```

→ **로그인한 사용자의 username 이 자동으로 `created_by` 에 박힌다**. 마스터 엔티티/서비스 코드는 **한 줄도 안 바꿔도** 작동한다. 이게 횡단 관심사를 따로 빼두는 가치.

### `Optional.of(...)` 가 의미하는 것

`AuditorAware` 의 시그니처가 `Optional<T> getCurrentAuditor()` 인 이유는 **"현재 감사 대상자가 없을 수도 있다"** 를 표현하기 위해서다(배치 작업, 시스템 초기화 등). Phase 1 에서는 항상 있는 셈이라 `Optional.of(...)` 를 쓴다. Phase 6 의 미인증 케이스에서는 `Optional.empty()` 가 돌아갈 수 있다.

---

## 🔥 `ClockConfig` — 시간을 의존성으로

```java
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
```

### 한 줄짜리 설정인데 왜 따로 두는가

```java
// 흔한 방식
LocalDate.now();         // 시스템 시계 직접 호출

// 이 프로젝트의 방식
LocalDate.now(clock);    // 주입받은 Clock 사용
```

`LocalDate.now()` 를 직접 부르면 **테스트할 때 시간을 고정할 방법이 없다**. 2025년 12월 31일 23:59 에 발급된 코드가 정말 `CUST-2025-` 인지, 자정 직후라면 `CUST-2026-` 인지 같은 경계 케이스를 결정적으로 테스트할 수 없다.

`Clock` 을 주입받으면 테스트에서 `Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)` 같은 걸 빈으로 대체해서 **시간을 통제**할 수 있다.

JDK가 시간을 인터페이스(`Clock`)로 추상화해둔 덕에, 외부 라이브러리 없이 시간 의존성을 깔끔하게 끊을 수 있는 것. 02편 `CodeGenerator` 가 이미 이걸 활용 중이다:

```java
int year = LocalDate.now(clock).getYear();   // CodeGenerator.java
```

### `systemDefaultZone()` 의 함정

운영에서 서버 타임존이 KST 인지 UTC 인지에 따라 `getYear()` 결과가 자정 근처에서 달라질 수 있다. 학습 프로젝트라 일단 `systemDefaultZone()` 으로 두지만, **실무에서는 `Clock.system(ZoneId.of("Asia/Seoul"))` 같이 명시적인 존을 박는다**. 그래야 운영-개발 서버 타임존이 달라도 동작이 같다.

→ Phase 4(SD↔MM 연계) 이후 다중 타임존을 다룰 일이 생기면 이 한 줄을 바꾸게 될 가능성이 높다.

---

## 🔥 `GlobalExceptionHandler` — 예외를 응답으로

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) { ... }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException ex) { ... }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) { ... }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) { ... }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) { ... }
}
```

### `@RestControllerAdvice` — 모든 컨트롤러에 공통 적용

이 어노테이션이 붙은 클래스의 `@ExceptionHandler` 메서드는 **앱 안의 모든 REST 컨트롤러에서 발생한 예외**를 받아 처리한다. 컨트롤러마다 try-catch 를 박지 않고도 일관된 응답을 만들 수 있다.

### `ProblemDetail` — RFC 9457 표준 에러 응답

`ProblemDetail` 은 Spring 6 (Boot 3) 에서 들어온 표준 에러 응답 객체다. JSON으로 직렬화하면 이런 모양:

```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "입력값 검증에 실패했습니다.",
  "fieldErrors": {
    "name": "이름은 필수입니다.",
    "businessNo": "사업자번호 형식이 아닙니다."
  }
}
```

직접 만든 에러 DTO를 안 쓰고 표준을 쓰는 이유 — 클라이언트 라이브러리, 다른 회사 API, 모니터링 도구들이 이 포맷을 기본으로 이해한다. **공통 언어를 쓰면 협업 비용이 준다**.

### 5가지 예외 매핑

| 예외 | HTTP 상태 | 의미 |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | `@Valid` 검증 실패 — 필드별 메시지 포함 |
| `EntityNotFoundException` | 404 | 조회 결과 없음 |
| `DataIntegrityViolationException` | 409 | UNIQUE/FK 위반 (중복 사업자번호 등) |
| `IllegalArgumentException` | 400 | 비즈니스 인자 오류 (`prefix is blank` 등) |
| `IllegalStateException` | 409 | 도메인 invariant 위반 (`code 는 한 번만 할당`) |

마지막 두 개가 흥미롭다. 자바 표준 `IllegalArgumentException` / `IllegalStateException` 을 그냥 도메인 예외처럼 던지고 받는다. 즉 `BaseEntityWithCode.assignCode(...)` 의 이 코드:

```java
if (this.code != null) {
    throw new IllegalStateException("code 는 한 번만 할당할 수 있다. ...");
}
```

이게 HTTP 409 `Conflict` 응답이 된다.

### 학습 단계의 단순화

javadoc 에 명시되어 있다:

> 학습 단계라 단순화: 비즈니스 도메인별 커스텀 예외 위계는 만들지 않고, 대표적인 4종(검증 실패 / 중복 / 미존재 / 잘못된 인자) 만 처리한다.

실무라면 `CustomerNotFoundException extends DomainException` 같은 위계를 만들고 HTTP 상태/메시지를 그쪽에서 결정하게 한다. Phase 1은 JDK 표준 예외 + 글로벌 핸들러로 줄여서 학습 부담을 낮춤. Phase 6/7 쯤 도메인 예외가 본격적으로 필요해지면 그때 위계를 도입한다.

### `log.warn(...)` 가 한 곳에만 있는 이유

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
    log.warn("Data integrity violation", ex);
    ...
}
```

다른 핸들러는 로그를 안 찍는다. 차이가 뭐냐:

- `EntityNotFoundException`, `IllegalArgumentException` → **클라이언트 잘못** (잘못된 ID, 잘못된 값). 정상 동작이라 로그가 의미 없음.
- `DataIntegrityViolationException` → **DB 제약 위반**. 코드/마이그레이션의 가정이 깨졌을 가능성이 있어서 운영자가 알아야 함.

→ 모든 예외에 로그를 찍지 말고, **"문제일 수도 있는" 예외만** 찍는다는 작은 원칙.

---

## 🔥 셋이 만나는 지점 — 시나리오로

```
POST /api/v1/customers
{ "name": "", "businessNo": "잘못된형식" }

  1. Spring Validation → 빈 name, 형식 위반 businessNo 발견
     → MethodArgumentNotValidException 발생
     → GlobalExceptionHandler.handleValidation(...)
     → 400 ProblemDetail 응답 (fieldErrors 포함)
```

```
POST /api/v1/customers
{ "name": "한빛", "businessNo": "123-45-67890" }
  (이미 같은 사업자번호 존재)

  1. CustomerService.create(...)
     → codeGenerator.nextCode("CUST")    [Clock 사용]
     → Customer.of("CUST-2026-0043", ...)
     → customerRepository.save(...)       [Auditing → created_by="system"]
     → DB INSERT → UNIQUE 위반
     → DataIntegrityViolationException
     → GlobalExceptionHandler.handleDataIntegrity(...)
     → 409 ProblemDetail 응답 + log.warn
```

3개의 작은 인프라가 한 요청 안에서 각자의 역할을 한다.

---

## 자기 점검

- [ ] `@EnableJpaAuditing` 을 끄면 `created_at` 컬럼은 어떻게 되는가? (NOT NULL 위반)
- [ ] Phase 6에서 `AuditorAware` 의 람다 본문만 바꿔도 마스터 코드가 그대로 작동하는 이유?
- [ ] `Clock` 을 따로 빈으로 두지 않고 `LocalDate.now()` 를 직접 부르면 어떤 테스트를 못 쓰는가?
- [ ] `ProblemDetail` 을 직접 만든 DTO 대신 쓰는 이유?

---

이전 편 → [02-코드-자동-생성.md](./02-코드-자동-생성.md)
다음 편 → [04-Customer-마스터-풀스택.md](./04-Customer-마스터-풀스택.md)
