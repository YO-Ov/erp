# 1/9. `docker-compose.yml` — 인프라

> **사이클 단계**: 6/7 (코드 워크스루)
> **읽는 순서**: 바깥(인프라) → 안쪽(코드) → 검증(테스트)

```yaml
services:
  mysql:
    image: mysql:8.0
    ports:
      - "3307:3306"
    environment:
      MYSQL_DATABASE: erp_db
      MYSQL_USER: erp
      MYSQL_PASSWORD: erp
      TZ: Asia/Seoul
    volumes:
      - hwlee-erp-mysql-data:/var/lib/mysql
    command:
      - --default-time-zone=+09:00
```

## 짚어야 할 4가지

### 1. 왜 `3307:3306`인가? (포트 매핑)

- 콜론 왼쪽 = **호스트(맥)** 포트, 오른쪽 = **컨테이너 내부** 포트
- 맥에 기존 MySQL이 깔려 있으면 3306이 점유될 가능성 → 호스트는 3307로 비킨다
- → 그래서 `application-local.yml`의 JDBC URL도 `localhost:3307`로 맞춰져 있음

### 2. 왜 `TZ: Asia/Seoul`과 `--default-time-zone=+09:00`을 둘 다 설정하나?

- `TZ`는 **컨테이너 OS** 시간대 (로그·스크립트가 보는 시간)
- `--default-time-zone`은 **MySQL 서버** 자체의 시간대 (NOW(), CURRENT_TIMESTAMP가 보는 시간)
- 둘이 다르면 회계 마감일이 어긋남 → **기간계는 시간대 사고가 가장 흔한 실무 버그**

### 3. `volumes: hwlee-erp-mysql-data:/var/lib/mysql` — 명명된 볼륨

- `docker compose down` 으로는 데이터 안 지워짐 (볼륨 유지)
- 완전히 초기화하려면 `docker compose down -v` (-v = volume 삭제)
- → Flyway 마이그레이션 처음부터 다시 돌리고 싶을 때 쓰는 명령

### 4. `healthcheck`로 `mysqladmin ping`

- 설계 제안서엔 없던 것을 추가한 부분. MySQL이 "기동만" 된 게 아니라 "쿼리 받을 준비"가 됐는지 확인
- 이후 Phase에서 다른 컨테이너(Kafka 등)가 MySQL에 의존할 때 `depends_on: condition: service_healthy`로 쓸 수 있게 미리 깔아둔 것
