# 현우전자(hyunwoo) ERP + MES 학습 프로젝트

ERP(기간계) + MES(현장계) 두 시스템을 별도 앱으로 만들고 **REST(동기) + Kafka(비동기)**로 연계하는 학습용 Spring Boot 모노레포.

자세한 학습 계획은 [`ERP-STUDY-PLAN.md`](ERP-STUDY-PLAN.md), 통합 아키텍처는 [`doc/11-phase-16-통합-운영/1-통합-아키텍처와-E2E.md`](doc/11-phase-16-통합-운영/1-통합-아키텍처와-E2E.md) 참고.

## 시스템 구성

```
hwlee-erp(8080, erp_db) ──① 작업지시 발행 (동기 REST, CircuitBreaker)──▶ hwlee-mes(8082, mes_db)
  계획·재무                                                                   현장 실행
  SD/MM/FI/HR/PP/배치/리포트   ◀─② 실적·불량 (비동기 Kafka + Outbox, 멱등)──   작업지시/실적/품질/설비
            └────── 공유 인프라: Kafka(KRaft), Zipkin(분산추적) ──────┘
```

## 디렉토리 구조

```
my-app/erp/                   ← 모노레포 루트
├─ ERP-STUDY-PLAN.md           학습 계획서
├─ PROGRESS.md                 진행 현황판
├─ docker-compose.yml          공유 인프라 (MySQL×2, Kafka(KRaft), Zipkin)
├─ doc/                        Phase별 도메인 학습 자료
├─ contracts/                  ERP-MES API/이벤트 계약 (openapi/, events/)
├─ hwlee-erp/                  ERP 프로젝트 (Phase 0~10 + MES 연계)
└─ hwlee-mes/                  MES 프로젝트 (Phase 11~16)
```

## 실행 방법

```bash
docker compose up -d                              # ① 인프라(MySQL×2, Kafka, Zipkin)
cd hwlee-erp && SPRING_PROFILES_ACTIVE=local ./gradlew bootRun   # ② ERP (8080)
cd hwlee-mes && SPRING_PROFILES_ACTIVE=local ./gradlew bootRun   # ③ MES (8082)
```

- ERP Swagger: http://localhost:8080/swagger-ui.html (로그인 `admin@hyunwoo.com` / `pass1234`)
- MES 현장 화면: http://localhost:8082/work-orders (로그인 없음)
- Zipkin(분산 추적): http://localhost:9411

## 학습 진행 현황 — 전체 완료 ✅

- [x] Phase 0~1: 환경 셋업 / 마스터 데이터
- [x] Phase 2~5: SD 영업 / MM 자재 / SD↔MM 연계 / FI 회계
- [x] Phase 6~8: 인증·권한·감사 / HR / PP 생산
- [x] Phase 9~10: 야간 배치 / 리포트·대시보드
- [x] Phase 11~12: MES 셋업·연계 인프라 / 작업지시 수신(REST)
- [x] Phase 13~14: 생산 실적 / 실적 전송(Kafka+Outbox)
- [x] Phase 15~16: 품질·설비 / 통합 시나리오·정합성

> 구현은 완료. 도메인 브리핑·코드 워크스루 등 학습 산출물은 `doc/` 에 점진 보강.

## 기술 스택

- Java 21, Spring Boot 3.5.14, Gradle Kotlin DSL
- MySQL 8.0, Spring Data JPA, Flyway
- Spring Security + JWT, Spring Batch, Query 집계(JPQL)
- **Kafka**(Outbox 패턴), **Resilience4j**(CircuitBreaker/Retry), **Micrometer Tracing + Zipkin**
- Testcontainers, springdoc-openapi(Swagger), Lombok

## 포트

| 항목 | 포트 |
|------|------|
| ERP 앱 | 8080 |
| MES 앱 | **8082** (8081은 RN 등과 충돌 회피) |
| ERP MySQL | 3307 |
| MES MySQL | 3308 |
| Kafka | 9092 |
| Zipkin | 9411 |
