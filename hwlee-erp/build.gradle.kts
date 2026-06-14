plugins {
	java
	id("org.springframework.boot") version "3.5.14"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.hwlee"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")

	// 로컬 개발 편의 — 템플릿/리소스 변경 시 자동 재시작 + LiveReload (운영 빌드 제외)
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	// Phase 9 — 배치 처리 (야간 마감). 메타테이블은 Flyway(V37)로 생성.
	implementation("org.springframework.boot:spring-boot-starter-batch")
	testImplementation("org.springframework.batch:spring-batch-test")

	// Phase 11 — MES 연계 인프라
	implementation("org.springframework.boot:spring-boot-starter-actuator")      // 헬스/지표
	implementation("org.springframework.kafka:spring-kafka")                     // MES→ERP 실적 수신 (Phase 14)
	implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")     // Circuit Breaker (ERP→MES 호출 보호, Phase 12)
	implementation("io.micrometer:micrometer-tracing-bridge-brave")             // 분산 추적
	implementation("io.zipkin.reporter2:zipkin-reporter-brave")                 // Zipkin 전송

	// Phase 6 — 인증/인가 + 화면
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")

	// Phase 6 — JWT (jjwt 0.12.x)
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-mysql")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("com.mysql:mysql-connector-j")
	annotationProcessor("org.projectlombok:lombok")
	implementation("org.mapstruct:mapstruct:1.6.3")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:mysql")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
