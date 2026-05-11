# 2/9. `build.gradle.kts` — 빌드/의존성

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.5.14"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.hwlee"
version = "0.0.1-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("com.mysql:mysql-connector-j")
    annotationProcessor("org.projectlombok:lombok")
    ...
}
```

## 짚어야 할 5가지

### 1. 두 개의 플러그인이 함께 동작하는 방식

- `org.springframework.boot` → 실행 가능한 fat JAR 만들기 (`bootRun`, `bootJar` 태스크 제공)
- `io.spring.dependency-management` → **Spring Boot가 검증한 라이브러리 버전 세트(BOM)를 자동으로 적용**
- 그래서 의존성 줄에 **버전 번호가 거의 없다** (`spring-boot-starter-web` 같은 것)
- → 본인이 직접 골라 쓴 것 = springdoc(2.6.0), testcontainers(BOM에 있긴 함)
- **학습 포인트**: ERP에서 라이브러리 호환성 사고는 흔하다. Boot BOM에 맡기면 사고가 줄어든다

### 2. `toolchain { languageVersion = 21 }` — 시스템 자바와 무관

- 시스템에 자바 17이 깔려 있어도 Gradle이 **자바 21을 자동 다운로드**해서 컴파일/실행에 사용
- 팀원마다 로컬 자바 버전이 달라도 빌드 결과가 같음 → 기간계 운영 환경 일치성에 필수

### 3. `compileOnly` vs `runtimeOnly` vs `annotationProcessor` (Scope 학습)

| Scope | 의미 | 예시 |
|---|---|---|
| `implementation` | 컴파일+실행 둘 다 필요 | `spring-boot-starter-web` |
| `compileOnly` | 컴파일에만 필요, 런타임엔 빠짐 | `lombok` (소스만 변환, 클래스파일엔 흔적 없음) |
| `runtimeOnly` | 런타임에만 필요 | `mysql-connector-j` (JDBC 드라이버는 컴파일 시 코드에 안 나타남, JVM이 런타임에 동적 로드) |
| `annotationProcessor` | 컴파일 시점에 **코드 생성** | `lombok` (`@Getter` → 실제 getter 메서드 생성) |

→ Lombok이 `compileOnly` + `annotationProcessor` 둘 다 있는 이유: 어노테이션을 인식하려면(컴파일러가 lombok JAR을 보아야 함) + 실제 코드를 생성하려면(annotation processor로 등록) 둘 다 필요

### 4. `starter-validation` — 뜬금없어 보이지만 Phase 1부터 필수

- Bean Validation (`@NotNull`, `@Size`, `@Pattern` 등) — 마스터 데이터 등록 시 입력 검증에 쓰임
- "고객번호는 `CUST-` + 4자리 숫자여야 한다" 같은 규칙을 어노테이션 한 줄로 표현

### 5. 아직 없는 것들 (Phase별 추가 예정)

- QueryDSL → Phase 2 (수주 다중 필터 조회 시작)
- MapStruct → Phase 1~2 (Entity ↔ DTO 변환)
- Spring Security → Phase 6 (인증)
- Spring Batch → Phase 9 (야간 마감)
- → **지금 미리 다 넣지 않는 이유**: "왜 이게 필요한가"를 체감하는 순간에 추가해야 학습이 된다. 처음부터 풀세트로 넣으면 의미를 못 느낌
