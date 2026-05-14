# 5/9. `HwleeErpApplication.java` — 애플리케이션 진입점

```java
package com.hwlee.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HwleeErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(HwleeErpApplication.class, args);
    }
}
```

코드는 13줄이지만, **이 한 클래스가 켜는 것**은 어마어마함.

---

## 🔥 `@SpringBootApplication` = 3개 어노테이션의 합성

펼치면 이렇다:

```java
@SpringBootConfiguration   // 이 클래스 = 설정 클래스
@EnableAutoConfiguration   // 의존성에 따라 자동으로 빈 등록 ⭐
@ComponentScan             // 이 클래스 패키지 하위를 스캔해서 @Component/@Service/@Controller 등록
```

이 셋 중 **`@EnableAutoConfiguration`이 Spring Boot의 본질**.

---

## 🔥 `@EnableAutoConfiguration`이 실제로 한 일 (이번 프로젝트 한정)

기동 시 클래스패스의 JAR들을 보고 "어떤 자동 설정을 켤지" 결정한다:

| 클래스패스에 있는 것 | 자동으로 켜진 것 |
|---|---|
| `spring-boot-starter-web` | Tomcat 8080 포트 시작, Jackson, MVC DispatcherServlet, `@RestController` 처리 |
| `spring-boot-starter-data-jpa` | EntityManagerFactory, TransactionManager, Hibernate, JPA Repository 스캐너 |
| `mysql-connector-j` (runtime) | JDBC 드라이버 자동 등록 |
| `flyway-core` | 기동 시 `db/migration/` 스캔 후 마이그레이션 실행 |
| `springdoc-openapi-...` | `/swagger-ui.html`, `/api-docs` 엔드포인트 |
| `starter-validation` | Bean Validation 처리기 (`@Valid` 동작) |

→ **개발자가 직접 빈을 등록한 게 아니라, "의존성을 추가했더니 알아서 켜졌다"**가 Spring Boot의 핵심.

**기동 시 콘솔에서 확인하는 법**:

```bash
./gradlew bootRun --debug
```

출력에 `CONDITIONS EVALUATION REPORT` → "어떤 자동 설정이 켜졌고, 어떤 게 안 켜졌고 왜인지" 다 나옴

---

## 🔥 `@ComponentScan`의 스캔 범위 = "이 클래스가 사는 패키지부터"

```
com.hwlee.erp/              ← @SpringBootApplication이 여기 있으므로
  ├─ HwleeErpApplication.java
  └─ common/health/         ← 이 하위는 자동으로 스캔됨
       └─ HealthController.java   (@RestController 인식)
```

**규약**: `@SpringBootApplication` 클래스는 **베이스 패키지의 최상위**에 둔다. 한 단계 아래에 두면 다른 패키지가 스캔에서 빠짐.

→ 그래서 `master/`, `sd/`, `mm/` 같은 향후 모듈을 모두 `com.hwlee.erp.master`, `com.hwlee.erp.sd`... 식으로 두는 패키지 구조가 강제됨.

---

## 🔥 `SpringApplication.run(...)` 한 줄이 하는 일 (시퀀스)

기동 시 시퀀스를 잘게 보면:

1. **환경 로딩** — `application.yml` + `application-local.yml`(`profiles: local`이므로) 읽기
2. **ApplicationContext 생성** — Spring의 DI 컨테이너 만들기
3. **자동 설정 평가** — `@EnableAutoConfiguration`이 클래스패스 보고 켤지 말지 결정
4. **빈 등록** — `@ComponentScan` + 자동 설정이 만든 빈들을 컨테이너에 채움
5. **Flyway 마이그레이션 실행** — JPA보다 **반드시 먼저** (`ddl-auto: validate`가 그 후에 검증)
6. **JPA `validate`** — Flyway가 만든 스키마와 엔티티가 일치하는지 검증 (지금은 엔티티가 없어 항상 통과)
7. **Tomcat 시작** — 8080 포트 listen
8. **준비 완료** — `Started HwleeErpApplication in X.X seconds`

→ **순서가 중요**: Flyway가 실패하면 7번까지 못 감 = 잘못된 스키마로 절대 기동 안 됨. 기간계 안전장치.

---

## 🔥 학습 포인트: "Spring Boot가 알아서 한다"의 함정

Spring Boot의 장점 = "알아서 다 해준다"
Spring Boot의 함정 = **"알아서 한 게 뭔지 모르면 디버깅 불가"**

ERP 실무에서 자주 겪는 사고들:

- "JPA가 이상한 SQL을 쏘는데 왜 그러지?" → 자동 설정된 `naming-strategy` 때문
- "트랜잭션이 안 걸려요" → 자동 설정된 `TransactionManager`가 어느 DataSource를 보는지 모름
- "트랜잭션이 두 번 커밋되는데요?" → 자동 설정으로 다중 DataSource가 들어가 있는데 인지 못 함

→ 학습 단계에서는 **"이 빈이 어디서 등록됐는가?"를 의식적으로 추적하는 습관**이 필요. IntelliJ에서 어노테이션 클릭 → 펼쳐서 확인. 또는 기동 시 `Bean Overview`를 직접 출력해보기:

```java
// 디버깅용 - 빈 목록 출력
@Bean
ApplicationRunner printBeans(ApplicationContext ctx) {
    return args -> Arrays.stream(ctx.getBeanDefinitionNames())
        .sorted().forEach(System.out::println);
}
```

---

## 비교: Spring Boot 없이 같은 일을 하려면?

- `web.xml` 작성
- `applicationContext.xml`에 `DataSource`, `EntityManagerFactory`, `TransactionManager` XML로 등록
- Tomcat 직접 깔고 WAR 배포
- Hibernate 설정 별도 XML
- 마이그레이션 도구 별도 통합

→ 옛날 Spring MVC + JPA 프로젝트는 **설정 XML이 수백 줄**. Spring Boot가 등장한 이유.
