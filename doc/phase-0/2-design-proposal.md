# Phase 0 — 설계 제안

> **사이클 단계**: 3/7 (모델링/설계 제안)
> **목표**: Phase 0에서 만들 9개 산출물의 구체적 설계를 결정하고 사용자가 승인한다
> **결정해야 할 항목 11개**는 끝의 "검토 포인트"에 표시

---

## A. 디렉토리 구조 (확정안)

```
my-app/erp/                                ← 모노레포 루트 (my-app은 IDE workspace)
├─ ERP-STUDY-PLAN.md                       (기존)
├─ LEARNING-LOG.md                         ← 신규: 사용자 학습 일지
├─ README.md                               ← 신규: 모노레포 안내
├─ docker-compose.yml                      ← 신규: 인프라 정의
├─ doc/                                    (기존, 학습 자료)
├─ contracts/                              ← 신규: Phase 11에서 채움 (지금은 .gitkeep만)
└─ hwlee-erp/                              ← 신규: ERP 프로젝트 루트
   ├─ build.gradle.kts
   ├─ settings.gradle.kts
   ├─ gradlew, gradlew.bat, gradle/        (Gradle wrapper)
   ├─ .gitignore
   ├─ src/main/
   │  ├─ java/com/hwlee/erp/
   │  │  ├─ ErpApplication.java
   │  │  └─ common/
   │  │     └─ health/
   │  │        └─ HealthController.java
   │  └─ resources/
   │     ├─ application.yml
   │     ├─ application-local.yml
   │     └─ db/migration/
   │        └─ V1__init.sql
   └─ src/test/
      └─ java/com/hwlee/erp/
         ├─ AbstractIntegrationTest.java   (Testcontainers 공통 베이스)
         └─ common/health/
            └─ HealthIntegrationTest.java
```

### 결정 사항
- **모노레포 방식**: Gradle 멀티 프로젝트가 아닌 **독립 디렉토리 방식**
  → MES 프로젝트(`hwlee-mes`)도 Phase 11에서 같은 레벨에 추가
  → 학습 단계에서는 두 시스템의 빌드/실행을 완전히 분리하는 게 더 직관적
- **베이스 패키지**: `com.hwlee.erp`

---

## B. `docker-compose.yml` 설계

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: hwlee-erp-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: erp_db
      MYSQL_USER: erp
      MYSQL_PASSWORD: erp
      TZ: Asia/Seoul
    ports:
      - "3307:3306"   # 호스트 3307 → 컨테이너 3306 (로컬 MySQL 충돌 회피)
    volumes:
      - hwlee-erp-mysql-data:/var/lib/mysql
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --default-time-zone=+09:00

volumes:
  hwlee-erp-mysql-data:
```

### 결정 사항
- **MySQL 8.0** (학습 환경 안정성을 위해 8.0 LTS, 8.4도 가능)
- **DB명**: `erp_db` / **사용자**: `erp` / **비밀번호**: `erp` (학습용)
- **포트**: `3306` (호스트 노출)
- **시간대**: `Asia/Seoul` 고정 (회계 데이터에 시간대 어긋나면 사고)
- **문자셋**: `utf8mb4` (이모지/한자 포함 안전)
- **데이터 영속화**: 명명된 볼륨 사용. `docker compose down`만으로는 데이터 안 지워짐
  → 초기화하려면 `docker compose down -v`
- **Phase 11에서 추가될 서비스**: kafka, zookeeper, zipkin, redis (지금은 mysql만)

---

## C. `build.gradle.kts` 의존성 (Phase 0 시점)

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.5.14"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.hwlee"
version = "0.0.1-SNAPSHOT"

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

repositories { mavenCentral() }

dependencies {
    // 핵심
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // DB & 마이그레이션
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // API 문서
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // 코드 생성
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // 테스트
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter:1.20.3")
    testImplementation("org.testcontainers:mysql:1.20.3")
}

tasks.withType<Test> { useJUnitPlatform() }
```

### 결정 사항
- **Spring Boot 3.5.14** (안정 버전)
- **Java 21** Toolchain
- **MapStruct, QueryDSL은 Phase 1, Phase 2에서 추가** (지금은 학습 단계 일치를 위해 미포함)

---

## D. `application.yml` 설계

### 기본 (`application.yml`)

```yaml
spring:
  application:
    name: hwlee-erp
  profiles:
    active: local

  datasource:
    url: jdbc:mysql://localhost:3307/erp_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: erp
    password: erp
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: validate          # ⭐ 절대 create/update 안 씀
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect
    open-in-view: false           # ⭐ 영속성 컨텍스트 누수 방지

  flyway:
    enabled: true
    baseline-on-migrate: false    # 신규 프로젝트라 baseline 불필요
    locations: classpath:db/migration

server:
  port: 8080

springdoc:
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: alpha
  api-docs:
    path: /api-docs

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE   # ? 파라미터 값 로그
```

### 핵심 결정 3가지 (학습 포인트)
1. **`ddl-auto: validate`** — Hibernate가 스키마를 못 만지게 함. 모든 변경은 Flyway로
2. **`open-in-view: false`** — 컨트롤러/뷰에서 LAZY 로딩이 일어나지 않게 함. 영속성 컨텍스트는 서비스 계층에서 닫음
3. **SQL 로깅 활성화** — 학습 단계에서 어떤 SQL이 나가는지 보이는 게 매우 중요

---

## E. 첫 Flyway 마이그레이션 — `V1__init.sql`

```sql
-- Phase 0: 스키마 마이그레이션 동작 확인용
-- Phase 1에서 실제 마스터 테이블이 추가되면서 V2로 이어짐
CREATE TABLE schema_init_marker (
    id BIGINT NOT NULL AUTO_INCREMENT,
    note VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO schema_init_marker(note) VALUES ('Phase 0 init OK');
```

### 결정 사항
- **파일명 규칙**: `V<버전>__<설명>.sql` (언더스코어 두 개)
- **InnoDB + utf8mb4** 명시 (MySQL 8 기본이지만 명시적으로)
- **Phase 1에서 V2부터 본격 마스터 테이블 추가**

---

## F. 헬스체크 API 명세

### 엔드포인트
```
GET /api/health
```

### 응답 (200 OK)
```json
{
  "status": "UP",
  "db": "OK",
  "timestamp": "2026-05-06T20:34:00+09:00"
}
```

### DB 장애 시 (503 Service Unavailable)
```json
{
  "status": "DOWN",
  "db": "FAIL: Connection refused",
  "timestamp": "2026-05-06T20:34:00+09:00"
}
```

### 결정 사항
- **DB 체크 방식**: `SELECT 1` 직접 실행 (DataSource로부터 Connection 받아서)
- **Spring Boot Actuator 미사용** (Phase 6에서 도입)
  → 이유: 직접 만들어 보면서 헬스체크가 어떤 동작인지 학습
- **응답에 timestamp 포함** — 시간대 처리 학습 차원

---

## G. Testcontainers 통합 테스트

### `AbstractIntegrationTest` (모든 통합 테스트의 베이스)

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("erp_db_test")
        .withUsername("erp")
        .withPassword("erp")
        .withReuse(true);   // 테스트 빠르게 (수동 재기동까지 컨테이너 재사용)

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
}
```

### `HealthIntegrationTest`

```java
class HealthIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void 헬스체크는_UP을_반환하며_DB도_OK다() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.db").value("OK"));
    }
}
```

### 결정 사항
- **테스트 메소드명을 한글로** (`@DisplayName` 대신) — 학습용으로 가독성 ↑
- **`withReuse(true)`** — 컨테이너를 매 테스트마다 띄우면 느림. 재사용 옵션 활성화
  (`~/.testcontainers.properties`에 `testcontainers.reuse.enable=true` 추가 필요)

---

## H. `LEARNING-LOG.md` 템플릿

```markdown
# 학습 일지

매 학습일 끝에 한 줄~한 문단씩 누적 작성.

## 2026-05-06 (Phase 0 시작)
- 새로 알게 된 것:
- 막힌 점:
- 자기 말로 정리:

## 2026-05-XX (다음 날)
...
```

---

## I. README.md 초안

```markdown
# 현우전자 ERP + MES 학습 프로젝트

ERP / MES 도메인 학습용 Spring Boot 모노레포.
자세한 내용은 [`ERP-STUDY-PLAN.md`](ERP-STUDY-PLAN.md) 참고.

## 실행 방법

### 1) 인프라 시작
\`\`\`bash
docker compose up -d
\`\`\`

### 2) ERP 애플리케이션 실행
\`\`\`bash
cd hwlee-erp
./gradlew bootRun
\`\`\`

### 3) Swagger UI 접속
http://localhost:8080/swagger-ui.html

### 4) 헬스체크 확인
\`\`\`bash
curl http://localhost:8080/api/health
\`\`\`

## 학습 진행 현황
- [x] Phase 0: 환경 셋업
- [ ] Phase 1: 마스터 데이터
- [ ] Phase 2: SD 영업
- [ ] ... (이하 ERP-STUDY-PLAN.md 참고)
```

---

## 검토 포인트 (사용자가 결정/확인해야 할 11개)

| # | 항목 | 제안 | 변경 가능 |
|---|------|------|----------|
| 1 | Java 버전 | **21** | 17도 가능 |
| 2 | Spring Boot 버전 | **3.5.14** | 3.4.x도 가능 |
| 3 | MySQL 버전 | **8.0** | 8.4도 가능 |
| 4 | DB 이름 | `erp_db` | 자유 |
| 5 | DB 사용자 / 비밀번호 | `erp` / `erp` (학습용) | 자유 |
| 6 | 호스트 포트 (앱) | `8080` | 충돌 시 변경 |
| 7 | 호스트 포트 (MySQL) | **`3307`** ✅ 확정 | 호스트 3307 → 컨테이너 3306 |
| 8 | 베이스 패키지 | `com.hwlee.erp` | 자유 |
| 9 | Gradle DSL | Kotlin DSL (`.kts`) | Groovy도 가능 |
| 10 | 모노레포 방식 | **독립 디렉토리** (Gradle 멀티 모듈 아님) | 멀티 모듈도 가능 |
| 11 | 시간대 | `Asia/Seoul` 고정 | 변경 가능하지만 고정 권장 |

각 항목에 대해 **"제안대로 OK"** 이거나 **"N번은 X로 바꿔줘"** 로 알려 주세요.

---

**다음 단계**: 사용자 승인 후 → **단계 5 (코드 구현)**.
AI가 위 9개 산출물(파일들)을 모두 작성하고, 단계 6에서 함께 워크스루합니다.
