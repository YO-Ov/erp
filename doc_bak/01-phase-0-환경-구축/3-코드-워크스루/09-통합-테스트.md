# 9/9. `TestHwleeErpApplication.java` + `HealthIntegrationTest.java` — 실행/검증

마지막은 두 파일을 함께 본다. 둘 다 **앞서 본 `TestcontainersConfiguration`을 활용하는 사용처**.

---

## A. `TestHwleeErpApplication.java` — 로컬 개발용 실행 진입점

```java
package com.hwlee.erp;

import org.springframework.boot.SpringApplication;

public class TestHwleeErpApplication {

    public static void main(String[] args) {
        SpringApplication.from(HwleeErpApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
```

### 🔥 1. `src/test/java`에 있지만 테스트가 아니다

JUnit `@Test`가 아닌 일반 `main()`. **테스트 폴더에 둔 이유**:

- `TestcontainersConfiguration`이 테스트 클래스패스에 있으므로 같은 곳에 둬야 import 가능
- 운영 빌드(`./gradlew bootJar`) 결과물에는 포함되지 않음 → 개발 편의용 코드가 운영 산출물 오염시키지 않음

### 🔥 2. `SpringApplication.from(...).with(...)` — Spring Boot 3.1+ 빌더

- `from(HwleeErpApplication::main)` → "원래 진입점인 `HwleeErpApplication`을 그대로 부르되..."
- `.with(TestcontainersConfiguration.class)` → "이 추가 설정을 얹어서"
- `.run(args)` → 실행

**결과**: `HwleeErpApplication`을 직접 실행한 것과 똑같이 기동되지만, `application-local.yml`의 `localhost:3307`이 아니라 **Testcontainers가 띄운 MySQL** 에 자동 연결.

### 🔥 3. 학습 단계에서의 의미: "Docker 명령 안 외워도 됨"

- `HwleeErpApplication.main()` 직접 실행 → `localhost:3307` 연결 시도 → 사전에 `docker compose up`을 해야 함
- **`TestHwleeErpApplication.main()` 실행 → Testcontainers가 알아서 MySQL을 띄움** → docker compose 명령 불필요

→ IntelliJ에서 두 클래스를 보면 둘 다 ▶️ 버튼이 보임. 학습 중 선호하는 방식 선택 가능.

### 🔥 4. 단, 학습 의도와 충돌하는 면도 있음

학습 계획서가 시연 단계에서 권한 명령은:

```bash
docker compose up -d
cd hwlee-erp && ./gradlew bootRun
```

→ "Docker 컴포즈로 인프라를 띄우고, 별도 프로세스로 앱을 실행한다"는 흐름이 운영 환경 멘탈모델과 같음. **`TestHwleeErpApplication`은 편의 도구**, **`docker compose + bootRun`은 학습 정공법**. 두 방식의 차이를 의식적으로 인지하는 게 좋다.

---

## B. `HealthIntegrationTest.java` — 첫 통합 테스트

```java
package com.hwlee.erp.common.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hwlee.erp.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HealthIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 헬스체크는_UP을_반환하며_DB도_OK다() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.db").value("OK"));
    }
}
```

### 🔥 1. 세 어노테이션 = 통합 테스트의 황금 세트

| 어노테이션 | 역할 |
|---|---|
| `@SpringBootTest` | **전체 Spring 컨텍스트 로딩**. 모든 빈을 만들고 자동 설정도 다 적용 (= 운영과 동일 환경) |
| `@AutoConfigureMockMvc` | `MockMvc` 빈을 등록 → HTTP 요청을 서버 없이 시뮬레이션 |
| `@Import(TestcontainersConfiguration.class)` | Testcontainers MySQL 컨테이너 등록 |

→ **Tomcat 없이도** `@RestController`가 진짜처럼 동작.

**`@SpringBootTest`의 무게**:

- 컨텍스트 로딩에 5~10초 (Phase 9쯤 가면 더 무거워짐)
- 따라서 단위 테스트(`@WebMvcTest`, `@DataJpaTest`)와 구분해서 씀:
  - `@DataJpaTest` → JPA 레이어만 (Phase 1에서 등장)
  - `@WebMvcTest` → 컨트롤러 레이어만 (서비스는 mock)
  - `@SpringBootTest` → 전체 통합 (지금처럼 끝~끝 검증)

### 🔥 2. `MockMvc` — HTTP 서버 없이 컨트롤러 호출

```java
mockMvc.perform(get("/api/health"))
```

- 실제 8080 포트 listen 안 함
- 요청 객체를 만들어 DispatcherServlet에 직접 주입
- 응답을 받아서 검증
- → **빠르고 격리됨**

비교 — `TestRestTemplate` (또는 `WebTestClient`):

- 진짜 포트로 띄워서 HTTP 요청을 쏨
- 더 운영 환경에 가깝지만 느림
- Phase 11에서 MES와 RestClient로 통신할 때 한 번 쓰게 될 것

### 🔥 3. `jsonPath("$.status")` — JSON 응답 파싱

```java
.andExpect(jsonPath("$.status").value("UP"))
.andExpect(jsonPath("$.db").value("OK"));
```

JSON 경로 표현식 (JsonPath 라이브러리):

- `$` = 루트
- `$.status` = 루트 객체의 `status` 필드
- `$.lines[0].itemCode` = 배열 첫 라인의 itemCode (Phase 2에서 등장)
- `$.lines[*].quantity` = 모든 라인의 quantity
- `$.lines.length()` = 배열 길이

→ Phase 2 수주 테스트에서 자주 등장:

```java
.andExpect(jsonPath("$.lines.length()").value(3))
.andExpect(jsonPath("$.totalAmount").value(1500000))
```

### 🔥 4. 한글 메서드명 — 학습용 결정

```java
void 헬스체크는_UP을_반환하며_DB도_OK다() throws Exception {
```

- Java에서 메서드명에 유니코드 사용 가능 (성능 영향 없음)
- 대안: `@DisplayName("헬스체크는 UP을 반환하며 DB도 OK다")` + 영문 메서드명
- 학습 단계에서는 한글 메서드명이 직관적 → 설계 제안서 결정 사항

**실무 평가**: 팀 컨벤션에 따름. 토스/우아한형제들 같은 곳도 한글 메서드명/DisplayName 적극 활용.

### 🔥 5. 이 테스트가 실제로 검증하는 것

표면적으로는 "헬스체크 API가 UP을 반환" 하지만, **암묵적으로 검증되는 것**:

- ✅ Spring Boot 컨텍스트가 성공적으로 기동됨 (자동 설정, Bean 의존성 모두 OK)
- ✅ Flyway가 V1__init.sql 마이그레이션 실행 (`@SpringBootTest` 시작 시점)
- ✅ JPA `validate`가 통과 (Flyway 결과 스키마와 엔티티가 일치 — 지금은 엔티티가 없음)
- ✅ DataSource가 HikariCP로 커넥션 풀 생성
- ✅ HealthController가 DI로 DataSource 주입받음
- ✅ DataSource가 Testcontainers MySQL에 실제 연결
- ✅ `SELECT 1` 쿼리 성공
- ✅ Jackson이 `HealthResponse`를 JSON으로 직렬화
- ✅ 응답이 `application/json`으로 반환

→ **단 한 줄 테스트가 시스템의 골격이 살아있는지** 전수 검증. 이게 "엔드 투 엔드 통합 테스트"의 가치.

### 🔥 6. 의도적으로 빠진 것 — DB 장애 케이스

설계 제안서에는 503 응답 케이스도 명세돼 있었지만 테스트 메서드는 UP 케이스만. **장애 시나리오 테스트가 어려운 이유**:

- DataSource를 일부러 망가뜨리기 어려움 (Testcontainers를 중간에 죽이면 다음 테스트도 영향)
- → 보통 `@MockBean`으로 DataSource를 가짜로 교체하거나, 별도 클래스로 분리

Phase 6 이후 "장애 시나리오 테스트" 패턴을 본격 학습 예정.

---

## 🔥 정리 — 9개 산출물의 의존 관계

```
docker-compose.yml ────┐  (운영 모드 인프라)
                       │
application.yml ───────┤  ┌──→ HwleeErpApplication ──→ HealthController ──→ HealthResponse
application-local.yml ─┤  │
build.gradle.kts ──────┴──┘
                          │
V1__init.sql ──────────── Flyway가 기동 시 실행
                          │
TestcontainersConfiguration ──┬──→ TestHwleeErpApplication (로컬 개발 편의)
                              └──→ HealthIntegrationTest (자동 검증)
```

- **운영 경로**: docker-compose → app yml → application → controller → SQL
- **테스트 경로**: TestcontainersConfiguration → Testcontainers MySQL → 같은 application + controller

---

## 🎯 워크스루 종합

9개 파일을 통해 학습한 것:

1. **Docker로 격리된 인프라** (시간대, 문자셋, 포트 매핑 결정)
2. **Gradle 의존성 스코프**와 Spring Boot BOM
3. **`ddl-auto: validate` + Flyway** — 기간계 스키마 관리 정공법
4. **마이그레이션 파일명 규칙**과 checksum 안전장치
5. **Spring Boot의 자동 설정 메커니즘** + 컴포넌트 스캔
6. **REST 컨트롤러의 한 사이클** (DispatcherServlet → 컨트롤러 → JSON 응답)
7. **`record` + 정적 팩토리 + `OffsetDateTime`** — 모던 자바 DTO 패턴
8. **`@ServiceConnection`** — Spring Boot 3.1의 Testcontainers 통합
9. **`@SpringBootTest` + `MockMvc` + JsonPath** — 통합 테스트 패턴
