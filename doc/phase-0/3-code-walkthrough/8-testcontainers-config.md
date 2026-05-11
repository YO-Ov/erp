# 8/9. `TestcontainersConfiguration.java` — 테스트 인프라

```java
package com.hwlee.erp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("erp_db_test")
                .withUsername("erp")
                .withPassword("erp")
                .withReuse(true);
    }
}
```

22줄짜리지만, **Spring Boot 3.1+의 가장 큰 테스트 개선점**이 응축돼 있음.

---

## 🔥 1. Testcontainers — "테스트마다 진짜 DB를 컨테이너로 띄운다"

```
JUnit 테스트 시작
   ↓
Testcontainers가 Docker에게 "mysql:8.0 컨테이너 띄워줘" 요청
   ↓
컨테이너가 랜덤 포트(예: 32847)로 떠서 준비 완료
   ↓
Spring Boot 컨텍스트가 그 포트로 DataSource 연결
   ↓
Flyway가 마이그레이션 실행 → 빈 DB에 V1__init.sql 적용
   ↓
실제 테스트 실행
   ↓
테스트 종료 → 컨테이너 정리 (또는 재사용)
```

**왜 H2 (인메모리 DB) 대신 진짜 MySQL?**

| | H2 인메모리 | Testcontainers MySQL |
|---|---|---|
| 속도 | 매우 빠름 | 느림 (컨테이너 기동 ~10초) |
| 정확성 | MySQL과 SQL 방언이 다름 | **운영 DB와 100% 동일** |
| MySQL 전용 기능 | ❌ (`AUTO_INCREMENT`, FULLTEXT INDEX 등) | ✅ |
| 마이그레이션 검증 | 일부 SQL 실패 | 운영 마이그레이션 그대로 검증 |

→ **ERP 기간계는 정확성 ≫ 속도**. H2에선 통과했는데 운영 MySQL에선 깨지는 SQL이 흔하다 (예: `GROUP BY`의 모드 차이, `JSON` 타입, 시간대 처리).

---

## 🔥 2. `@TestConfiguration` — 테스트 전용 빈 설정

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
```

`@Configuration`과의 차이:

- `@Configuration` → 컴포넌트 스캔에 잡힘 = 운영 코드에도 적용됨
- `@TestConfiguration` → **스캔에서 빠짐**. `@Import`로 명시적으로 가져와야 활성화
- → 테스트 인프라가 운영 코드를 오염시키지 않게 격리

**`proxyBeanMethods = false`**:

- `@Configuration` 클래스는 기본적으로 CGLIB 프록시로 감싸짐 (`@Bean` 메서드끼리 호출할 때 같은 인스턴스 보장)
- 여기선 그럴 일이 없으니 끔 → 클래스로딩 속도 ↑, 메모리 ↓
- 마이크로한 최적화지만, "프록시가 필요 없다"는 의도 표현으로도 의미가 있음

---

## 🔥 3. `@ServiceConnection` — Spring Boot 3.1의 마법 ⭐

```java
@Bean
@ServiceConnection
MySQLContainer<?> mysqlContainer() { ... }
```

이 한 줄이 **3.0 이전에 수십 줄로 하던 일을 대체**한다.

### 옛날(3.1 이전) 방식

```java
@DynamicPropertySource
static void overrideProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    // 추가로 driver-class-name, hikari.maximum-pool-size 등...
}
```

- 컨테이너의 랜덤 포트 → DataSource URL을 수동으로 덮어써야 함
- Redis, Kafka 추가 시 각각 또 코드 작성

### 지금(3.1+) 방식

```java
@ServiceConnection  // ← 끝
```

- Spring Boot가 `MySQLContainer` 타입을 인식 → "아, 이건 MySQL이구나"
- 컨테이너에서 JDBC URL/사용자/비번을 자동으로 뽑아내서 `DataSource` 빈에 자동 연결
- Phase 11에서 Kafka 컨테이너 추가 시 → `@ServiceConnection` 한 줄로 `KafkaTemplate` 자동 설정
- Phase 14에서 Redis 추가 시 → 마찬가지

→ **"테스트 인프라 코드가 도메인 코드에 침범하지 않게"** 만든 Spring Boot 3의 디자인 선택.

---

## 🔥 4. `MySQLContainer<?>` — 와일드카드와 메서드 체이닝

```java
return new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
        .withDatabaseName("erp_db_test")
        .withUsername("erp")
        .withPassword("erp")
        .withReuse(true);
```

**1) `<?>` 와일드카드의 이유**

- `MySQLContainer`는 `SELF` 타입 패턴(F-bound generic)을 쓰는 클래스: `class MySQLContainer<SELF extends MySQLContainer<SELF>>`
- 메서드 체이닝이 같은 타입을 반환하게 만드는 트릭 — 실무에서 자주 만나는 패턴
- 우리가 상속해서 쓸 일이 없으니 `<?>`로 그냥 둔다 (다이아몬드 연산자 `<>`가 추론)

**2) `withDatabaseName("erp_db_test")`**

- 운영 DB는 `erp_db`, 테스트는 `erp_db_test` → **이름으로 구분**
- 같은 호스트에서 동시에 띄워도 충돌 방지

**3) `withReuse(true)` — 학습 단계의 결정적 차이**

- 기본 동작: 테스트 종료 시 컨테이너 삭제 → 다음 실행 시 또 10초 기동
- `withReuse(true)`: 컨테이너를 살려둠 → 다음 실행 시 즉시 사용 (1~2초)
- **단**, `~/.testcontainers.properties`에 `testcontainers.reuse.enable=true`도 켜야 동작

→ 학습 중 테스트를 수십 번 돌리는데 매번 10초씩 기다리면 학습 흐름이 끊긴다. CI에서는 끔 (격리성 우선).

**`docker ps`로 확인 가능**:

- `withReuse(true)`로 한번 띄우면 → 테스트 끝나도 `docker ps`에 `testcontainers-...` 라벨이 붙은 컨테이너가 남아 있음
- 수동 정리: `docker rm -f $(docker ps -aq --filter "label=org.testcontainers")`

---

## 🔥 5. 베이스 패키지 위치 (`com.hwlee.erp` 최상위)

```
src/test/java/com/hwlee/erp/
  ├─ TestcontainersConfiguration.java   ← 여기 (최상위)
  ├─ TestHwleeErpApplication.java
  └─ common/health/
       └─ HealthIntegrationTest.java
```

- 베이스 패키지에 두면 어디서든 `@Import`로 끌어쓰기 좋음
- 모든 통합 테스트에서 재사용

---

## 🔥 6. 이 파일이 **두 가지 모드**에서 쓰인다

### 모드 1: 단위/통합 테스트 (JUnit)

```java
@SpringBootTest
@Import(TestcontainersConfiguration.class)  // ← 명시적 임포트
class HealthIntegrationTest { ... }
```

### 모드 2: 개발 시 로컬 실행 (9/9에서 봄)

```java
public class TestHwleeErpApplication {
    public static void main(String[] args) {
        SpringApplication.from(HwleeErpApplication::main)
            .with(TestcontainersConfiguration.class)  // ← 같은 클래스 재사용
            .run(args);
    }
}
```

→ **테스트 인프라와 로컬 개발 환경이 같은 코드로 동작.** 즉, `docker compose up`을 안 해도 IDE에서 `TestHwleeErpApplication`을 실행하면 Testcontainers가 알아서 MySQL을 띄움. Spring Boot 3.1의 핵심 DX 개선.

---

## Phase별 이 파일의 진화

| Phase | 추가될 컨테이너 |
|---|---|
| 0 (지금) | MySQL |
| 6 | Redis (인증 토큰 캐시) — `@ServiceConnection` Redis 컨테이너 추가 |
| 11 | Kafka, Zookeeper — MES 연계 |
| 16 | Zipkin (선택) — 분산 추적 |

각 컨테이너마다 `@Bean` 메서드 + `@ServiceConnection` 한 쌍씩 추가하면 끝.
