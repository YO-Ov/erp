# syntax=docker/dockerfile:1
# ─────────────────────────────────────────────────────────────
# ERP 멀티스테이지 빌드
#   1단계(build): JDK + gradle wrapper 로 실행 가능한 fat jar 생성
#   2단계(run)  : JRE 만 담아 경량 실행 이미지
# 베이스 이미지는 멀티아치(amd64/arm64) → Oracle A1(ARM)에서 그대로 빌드됨.
# ─────────────────────────────────────────────────────────────

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# ① 의존성 캐시 레이어 — 빌드 스크립트/래퍼만 먼저 복사해 받아둠.
#    (src 가 바뀌어도 의존성 레이어는 캐시 재사용 → 재빌드 빠름)
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

# ② 소스 복사 후 fat jar 빌드 (테스트는 배포 빌드에서 제외)
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ─── 실행 이미지 ───
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
