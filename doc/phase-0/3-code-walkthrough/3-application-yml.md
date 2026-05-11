# 3/9. `application.yml` + `application-local.yml` — 설정

## 두 파일이 분리된 이유부터

**`application.yml`** = 환경 무관한 설정 (JPA 정책, Flyway, 로깅, 포트)
**`application-local.yml`** = 환경별 설정 (DB 접속 정보) — `spring.profiles.active: local`로 자동 합쳐짐

→ 추후 `application-dev.yml`, `application-prod.yml`을 추가하면, 운영 DB 비밀번호 같은 민감 정보를 환경별로 격리할 수 있음. **기간계는 "운영 DB URL이 dev 코드에 박혀 있다가 사고나는 케이스"가 흔함.**

---

## 🔥 핵심 3가지 결정 (모두 ERP 학습 포인트)

### 1. `ddl-auto: validate` — Hibernate는 스키마를 절대 못 만진다

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

옵션은 5가지:

| 값 | 동작 | 학습용? | 실무? |
|---|---|---|---|
| `none` | 아무것도 안 함 | ❌ | ✅ |
| `validate` | 엔티티 ↔ 실제 DB 스키마가 **일치하는지 검증만**. 다르면 기동 실패 | ✅ | ✅ |
| `update` | 엔티티에 새 필드 추가하면 DB 컬럼도 자동 추가 | ✅ (예전 인강) | ❌ |
| `create` | 매번 기동 시 테이블 다 지우고 다시 만듦 | ❌ | ❌ |
| `create-drop` | `create` + 종료 시 다시 drop | 테스트 한정 | ❌ |

**왜 `validate`인가?**

- ERP는 **데이터가 자산**이다. Hibernate가 자동으로 `ALTER TABLE`을 날리는 건 사고의 시작
- 모든 스키마 변경은 **Flyway 마이그레이션 파일(`V2__...sql`, `V3__...sql`)** 로만 한다 → 코드 리뷰 대상이 되고, 운영 DB에 어떤 순서로 적용됐는지 `flyway_schema_history` 테이블에 기록됨
- `validate`는 "엔티티에 `name` 필드를 추가했는데 마이그레이션을 깜빡했다" 같은 경우 **기동 실패로 알려줌**

### 2. `open-in-view: false` — 영속성 컨텍스트는 서비스에서 닫는다

```yaml
spring:
  jpa:
    open-in-view: false
```

기본값은 `true`. 켜져 있으면 **HTTP 요청이 끝날 때까지 영속성 컨텍스트(EntityManager)가 살아 있다.** 이게 왜 위험한가?

```java
// open-in-view: true 일 때
@GetMapping("/orders/{id}")
public OrderDto getOrder(@PathVariable Long id) {
    Order order = orderService.find(id);  // 서비스 트랜잭션은 여기서 끝
    return OrderDto.from(order);          // ← 컨트롤러에서 LAZY 로딩이 일어남!
}
```

- 서비스가 `@Transactional`로 가져온 `order.getLines()`가 **컨트롤러에서 처음 호출되면 SQL이 나간다**
- 한 요청당 SQL이 몇 개 나가는지 예측 불가 = **N+1 사고**
- 트랜잭션 경계 밖에서 DB 커넥션을 쥐고 있어서 커넥션 풀 고갈

**`false`로 두면**:

- 컨트롤러에서 LAZY 로딩 시도 → `LazyInitializationException` 즉시 발생
- → "필요한 데이터는 서비스에서 명시적으로 fetch하라"는 규율을 코드가 강제함
- ERP는 객체 그래프가 복잡(주문→라인→상품→가격...)해서 이게 특히 중요

### 3. SQL 로깅 — 학습 단계에서는 **반드시** 켠다

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE
```

- `org.hibernate.SQL: DEBUG` → 어떤 SQL이 나갔는가
- `org.hibernate.orm.jdbc.bind: TRACE` → **`?` 자리에 어떤 값이 바인딩됐는가**

→ 워크스루의 핵심: "내가 짠 자바 코드 한 줄이 DB에 몇 개의 SQL을 쏘는지" 매번 눈으로 확인. 운영에서는 보통 `INFO` 이상으로 둠 (성능, 보안 이슈)

---

## Flyway 설정

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: false
    locations: classpath:db/migration
```

- `baseline-on-migrate: false` — **기존 DB에 마이그레이션을 처음 도입할 때 켜는 옵션**. 신규 프로젝트라 꺼둠
- `locations: classpath:db/migration` → `V1__init.sql`이 살고 있는 곳

---

## Swagger UI

```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /api-docs
```

- `/swagger-ui.html` → 브라우저에서 보는 화면
- `/api-docs` → 기계가 읽는 OpenAPI 스펙 JSON (Phase 11에서 MES와 계약 공유할 때 이게 핵심)
