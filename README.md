# 현우전자 ERP + MES 학습 프로젝트

ERP / MES 도메인 학습용 Spring Boot 모노레포.

자세한 학습 계획은 [`ERP-STUDY-PLAN.md`](ERP-STUDY-PLAN.md) 참고.

## 디렉토리 구조

```
my-app/erp/                   ← 모노레포 루트
├─ ERP-STUDY-PLAN.md           학습 계획서
├─ LEARNING-LOG.md             학습 일지 (사용자가 작성)
├─ README.md                   (이 파일)
├─ docker-compose.yml          공유 인프라 (MySQL 등)
├─ doc/                        Phase별 도메인 학습 자료
├─ contracts/                  ERP-MES API 계약 (Phase 11부터)
├─ hwlee-erp/                  ERP 프로젝트 (Phase 0~10)
└─ hwlee-mes/                  MES 프로젝트 (Phase 11~16, 미생성)
```

## 실행 방법

### 1) 인프라 시작
```bash
docker compose up -d
```

### 2) ERP 애플리케이션 실행
```bash
cd hwlee-erp
./gradlew bootRun
```

### 3) Swagger UI 접속
http://localhost:8080/swagger-ui.html

### 4) 헬스체크 확인
```bash
curl http://localhost:8080/api/health | jq
```

기대 응답:
```json
{
  "status": "UP",
  "db": "OK",
  "timestamp": "2026-05-06T20:34:00+09:00"
}
```

### 5) 통합 테스트 실행
```bash
cd hwlee-erp
./gradlew test
```

## 학습 진행 현황

- [x] Phase 0: 환경 셋업
- [ ] Phase 1: 마스터 데이터
- [ ] Phase 2: SD 영업
- [ ] Phase 3: MM 자재
- [ ] Phase 4: SD ↔ MM 연계
- [ ] Phase 5: FI 회계
- [ ] Phase 6: 인증/권한 + 감사
- [ ] Phase 7: HR
- [ ] Phase 8: PP 생산
- [ ] Phase 9: 야간 배치
- [ ] Phase 10: 리포트
- [ ] Phase 11~16: MES + ERP-MES 연계

## 기술 스택

- Java 21, Spring Boot 3.5.14, Gradle Kotlin DSL
- MySQL 8.0, Spring Data JPA, Flyway
- Testcontainers, springdoc-openapi (Swagger UI)
- Lombok
- (Phase 별로 QueryDSL, Spring Security, Spring Batch, Kafka 등 추가 예정)

## 포트

| 항목 | 포트 |
|------|------|
| ERP 앱 | 8080 |
| MySQL (호스트) | **3307** (호스트 3307 → 컨테이너 3306) |
| MES 앱 | 8081 (Phase 11 이후) |
