# 5/6. Flyway V21~V22 — ALTER 와 3단계 backfill

> 새 테이블이 아니라 **이미 있는 테이블에 컬럼을 더하는** 마이그레이션. Phase 4 는 SD↔MM 을 잇느라 두 기존 테이블(`goods_issue`, `delivery`)에 연결 컬럼을 추가한다.
> 설계서 §8 "마이그레이션 계획 (Flyway V21~V22)" 의 실제 구현. 핵심 학습 주제는 **운영 중 스키마 변경의 정석 — nullable→backfill→NOT NULL 3단계**.

대상 파일:

```
hwlee-erp/src/main/resources/db/migration/
├─ V19__create_goods_issue.sql      (원본 — 맥락)
├─ V20__seed_mm_demo.sql            (시드 — 맥락)
├─ V21__add_delivery_id_to_goods_issue.sql   ⭐ 이 편
└─ V22__add_warehouse_to_delivery.sql        ⭐ 이 편
```

---

## 🔥 지금까지와 다른 것 — CREATE 가 아니라 ALTER

V15~V20 까지 Phase 3 의 마이그레이션은 전부 `CREATE TABLE` 이었다. 빈 테이블을 새로 만드는 일은 쉽다 — 기존 데이터가 없으니 어떤 제약을 걸어도 충돌하지 않는다.

Phase 4 는 다르다. 새 테이블을 만들지 않는다. 대신 **이미 운영 중일 수 있는 두 테이블**에 컬럼 한 개씩을 추가한다:

| 버전 | 대상 테이블 | 추가 컬럼 | nullable? |
| --- | --- | --- | --- |
| V21 | `goods_issue` (V19 생성) | `delivery_id` | **nullable** (수동 GI 는 NULL) |
| V22 | `delivery` (Phase 2 V11 생성) | `warehouse_id` | **NOT NULL** (3단계 강화) |

→ Phase 4 가 "새 테이블 없이 코드만 추가하면 끝날 가능성" (Phase 3 워크스루 06편 끝부분 예상) 에 거의 근접했다. 새 도메인 테이블은 0개, 두 컬럼만 보강.

> 💡 ALTER 마이그레이션의 본질적 어려움 — `CREATE` 는 백지에 그리지만 `ALTER` 는 **이미 누가 살고 있는 집을 개조**하는 일. 기존 행이 새 제약을 만족하는지가 늘 문제. V21 은 nullable 이라 쉽고, V22 는 NOT NULL 이라 3단계가 필요하다.

---

## 🔥 V21 — `goods_issue.delivery_id` (nullable FK 의 쉬운 길)

```sql
-- Phase 4: GoodsIssue 가 자기 출처(Delivery) 를 강한 FK 로 참조.
--
-- 비즈니스 규칙:
--  - 출하 확정 시 DeliveryEventListener 가 자동 생성하는 GI 는 delivery_id 가 채워진다.
--  - 사용자가 직접 등록한 GI(실사 조정/폐기 등)는 delivery_id = NULL.
--  - nullable FK — 두 출처(자동/수동)가 한 테이블에 공존하며, NULL 여부로 구분된다.
--  - JPA 매핑은 Long 컬럼(약한 객체 참조) — MM 이 SD 엔티티 클래스를 import 하지 않도록
--    모듈 경계를 지킨다. 정합성은 아래 DB FK 제약이 강제한다.

ALTER TABLE goods_issue
    ADD COLUMN delivery_id BIGINT NULL COMMENT '출하 자동 발생 시 원천 Delivery (수동 등록은 NULL)';

ALTER TABLE goods_issue
    ADD CONSTRAINT fk_goods_issue_delivery FOREIGN KEY (delivery_id) REFERENCES delivery(id);

CREATE INDEX idx_goods_issue_delivery ON goods_issue(delivery_id);
```

세 문장의 의미:

1. **`ADD COLUMN delivery_id BIGINT NULL`** — 컬럼 추가. 핵심은 `NULL`. 기존 `goods_issue` 행이 단 하나라도 있으면, NOT NULL 컬럼을 기본값 없이 추가할 수 없다 (그 행의 새 컬럼 값을 무엇으로 채울지 DB 가 모름). nullable 이면 기존 행은 모두 `NULL` 로 채워지고 ALTER 가 무사히 끝난다.
2. **`ADD CONSTRAINT fk_goods_issue_delivery FOREIGN KEY ... REFERENCES delivery(id)`** — 강한 FK. `delivery_id` 가 채워졌다면 반드시 실존하는 `delivery.id` 여야 한다. FK 컬럼이 nullable 이면 `NULL` 인 행은 FK 검사를 면제받는다 (SQL 표준 — NULL 은 "참조 안 함" 으로 간주). 그래서 수동 GI(NULL) 와 자동 GI(실존 delivery) 가 한 제약 아래 공존.
3. **`CREATE INDEX idx_goods_issue_delivery`** — "이 Delivery 가 만든 GI 찾기" 역방향 조회용. 출하 취소 등에서 `WHERE delivery_id = ?` 를 빠르게.

### 왜 `delivery_id` 가 nullable 인가 (비즈니스 의미)

`goods_issue` 한 테이블에 **두 출처의 출고가 공존**한다:

| 출처 | delivery_id | 발생 경로 |
| --- | --- | --- |
| SD 출하 자동 | 실존 Delivery id | 출하 확정 → `DeliveryEventListener` 가 GI 자동 생성 |
| 사용자 수동 | `NULL` | 실사 조정 / 폐기 (SHIPMENT 아닌 ADJUSTMENT/SCRAP) |

→ `delivery_id IS NULL` 여부가 곧 "이 출고가 어디서 왔는가" 의 구분자. 두 경우를 별도 테이블로 쪼개지 않고 nullable 컬럼 한 개로 표현 — Phase 3 의 `goods_issue.reason` (SHIPMENT/ADJUSTMENT/SCRAP) 과 같은 "한 테이블, 한 컬럼으로 갈래 나누기" 철학.

> 💡 약한 객체 참조 — JPA 매핑은 `Long deliveryId` 컬럼으로만 (MM 이 SD 의 `Delivery` 엔티티를 `import` 하지 않음). 모듈 경계를 코드에서 지키되, **정합성은 DB FK 가 강제**. Phase 3 의 `stock_movement.ref_type/ref_id` 가 FK 를 일부러 안 건 약한 참조였던 것과는 반대 — 여기선 출처가 `Delivery` 하나로 고정이라 강한 FK 를 걸 수 있다.

---

## 🔥 V22 — `delivery.warehouse_id` (3단계 패턴이 핵심)

이 편의 진짜 학습 포인트. NOT NULL 컬럼을 **기존 행이 있는** 테이블에 안전하게 추가하는 정석.

```sql
-- Phase 4: Delivery 가 출하 창고를 직접 안다 (DeliveryShippedEvent 본문에 담아 MM 으로 전달).
--
-- Phase 2 모델은 출하지 창고 개념이 없었다 — 출하가 어느 창고에서 나가는지 알 수 없으면
-- MM 의 재고 차감 대상 창고를 특정할 수 없으므로, Phase 4 에서 보강한다.
--
-- 모듈 경계: JPA 는 Long warehouse_id 로만 매핑한다 (SD 가 MM 의 Warehouse 엔티티를 import 하지 않음).
-- 정합성은 아래 DB FK 제약이 강제하고, 존재 검증은 출하 시점에 MM 리스너가 수행한다.

-- 1) 먼저 nullable 로 추가 (기존 행이 있어도 ALTER 가 실패하지 않도록)
ALTER TABLE delivery
    ADD COLUMN warehouse_id BIGINT NULL COMMENT '출하지 창고';

-- 2) 기존 행(Phase 2 시연 중 생성됐을 수 있음)은 본사창고로 backfill (학습 환경 가정)
UPDATE delivery
   SET warehouse_id = (SELECT id FROM warehouse WHERE code = 'WH-HQ')
 WHERE warehouse_id IS NULL;

-- 3) NOT NULL 강제 — 이후 모든 출하는 창고를 반드시 가진다
ALTER TABLE delivery
    MODIFY COLUMN warehouse_id BIGINT NOT NULL COMMENT '출하지 창고';

ALTER TABLE delivery
    ADD CONSTRAINT fk_delivery_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse(id);

CREATE INDEX idx_delivery_warehouse ON delivery(warehouse_id);
```

### 왜 컬럼이 필요한가 (비즈니스)

Phase 2 의 `Delivery` 는 "어느 창고에서 나가는가" 개념이 아예 없었다 — 출하는 그냥 출하였다. 그런데 Phase 4 에서 SD↔MM 을 잇는 순간, 출하가 **MM 의 어느 창고 재고를 차감할지** 특정해야 한다. 그 정보를 담을 곳이 `delivery.warehouse_id`. 출하 확정 시 `DeliveryShippedEvent` 본문에 실려 MM 리스너로 전달된다.

→ 모델의 "구멍" 을 Phase 가 진행되며 메우는 전형. Phase 2 에서는 필요 없던 정보가 모듈 연계로 필수가 됨.

---

## 🔥 3단계의 각 단계 — 왜 이 순서인가

### (1) `ADD COLUMN ... NULL` — 먼저 비워서 추가

```sql
ALTER TABLE delivery
    ADD COLUMN warehouse_id BIGINT NULL COMMENT '출하지 창고';
```

기존 `delivery` 행이 있어도 ALTER 가 실패하지 않는다. 모든 기존 행의 `warehouse_id` 는 `NULL` 로 채워진다. 이 시점엔 NOT NULL 도 FK 도 없다 — 그냥 빈 컬럼 하나가 생긴 상태.

### (2) `UPDATE ... backfill` — 빈 칸을 채워 넣기

```sql
UPDATE delivery
   SET warehouse_id = (SELECT id FROM warehouse WHERE code = 'WH-HQ')
 WHERE warehouse_id IS NULL;
```

1단계에서 `NULL` 이 된 기존 행들을 **본사창고(WH-HQ)** 로 메운다. `WHERE warehouse_id IS NULL` 로 아직 안 채워진 행만 대상.

- `(SELECT id FROM warehouse WHERE code = 'WH-HQ')` — WH-HQ 의 PK 를 동적 조회. 환경마다 auto_increment id 가 다를 수 있으니 코드로 찾는다.
- **WH-HQ 가 반드시 존재함을 V20 시드가 보장** — V20 의 첫 INSERT 가 WH-HQ 본사창고였다 (Phase 3 워크스루 06편). 그래서 이 서브쿼리는 NULL 을 반환하지 않는다. 만약 WH-HQ 가 없으면 `warehouse_id` 가 NULL 로 남고 다음 3단계의 NOT NULL 강제에서 실패했을 것.

### (3) `MODIFY COLUMN ... NOT NULL` — 제약 강화

```sql
ALTER TABLE delivery
    MODIFY COLUMN warehouse_id BIGINT NOT NULL COMMENT '출하지 창고';
```

이제 모든 행이 `warehouse_id` 를 가졌으므로 (1단계 신규 NULL → 2단계 backfill 로 채움) NOT NULL 을 걸어도 위반하는 행이 없다. 이후 INSERT 되는 모든 출하는 창고를 **반드시** 가진다. 이어서 FK 제약과 인덱스 추가.

---

## 🔥 왜 바로 NOT NULL 로 못 추가하나

만약 한 줄로 시도했다면:

```sql
-- ❌ 기존 행이 하나라도 있으면 실패
ALTER TABLE delivery
    ADD COLUMN warehouse_id BIGINT NOT NULL;
```

기존 `delivery` 행의 새 컬럼 값을 무엇으로 채울지 DB 가 알 수 없다. NOT NULL 인데 기본값(`DEFAULT`)도 없으니 채울 게 없다 → ALTER 실패. (MySQL 이 암묵적 기본값 0 을 넣어줄 수도 있지만, 그건 **의미 없는 창고 id 0** 으로 FK 위반을 부르므로 더 나쁘다.)

→ 그래서 **nullable 추가 → backfill → NOT NULL 강제** 의 3단계가 운영 마이그레이션의 정석:

| 단계 | 한 일 | 만약 건너뛰면 |
| --- | --- | --- |
| ① nullable ADD | 빈 컬럼 추가, 기존 행은 NULL | (바로 NOT NULL 추가) → 기존 행 채울 값 없어 ALTER 실패 |
| ② backfill UPDATE | NULL 행을 의미 있는 값으로 채움 | 다음 단계 NOT NULL 강제에서 NULL 행이 위반 |
| ③ NOT NULL MODIFY | 제약 강화, 이후 INSERT 강제 | 컬럼이 영원히 nullable — 빠뜨린 출하가 생길 여지 |

> ⚠️ **운영 주의** — 실제 운영 DB 였다면 backfill 의 **정확도**가 진짜 문제다. "기존 출하들이 어느 창고에서 나간 건가?" 를 정말로 알아내야 한다 (주문 지역? 담당자? 마지막 입고 창고?). 학습 환경은 창고가 WH-HQ 하나뿐이라 "전부 본사창고" 로 단순화했을 뿐. 운영이라면 이 UPDATE 가 한 줄짜리 서브쿼리로 끝나지 않는다.

---

## 🔥 시드 충돌 없음 — backfill 이 사실상 no-op

설계서 §8.3 의 핵심: V21/V22 의 ALTER 가 시드 데이터와 충돌하지 않는다.

```
V11 (Phase 2) — delivery 테이블 생성, 단 Delivery 행은 시드 안 함
V14 (Phase 2 시드) — Quotation + SalesOrder 만 만듦, Delivery 0행
V19 (Phase 3) — goods_issue 테이블 생성
V20 (Phase 3 시드) — Warehouse + Stock + StockMovement + GoodsReceipt, GoodsIssue 0행
```

- **V14 는 Delivery 행을 안 만든다** → V22 의 `ADD COLUMN` 시점에 `delivery` 가 비어 있다 (시연 중 사용자가 직접 만들지 않은 한). backfill `UPDATE` 의 대상 행이 0개 → **no-op** (영향받는 행 0). 그래도 3단계를 다 거치는 이유 — 시연 중 누가 Delivery 를 만들었을 수도 있고, 무엇보다 **마이그레이션은 데이터 유무와 무관하게 항상 안전해야** 하므로.
- **V20 은 GoodsIssue 행을 안 만든다** → V21 의 `ADD COLUMN delivery_id` 시점에 `goods_issue` 가 비어 있다. nullable 이라 어차피 안전.

→ 두 ALTER 모두 "기존 행이 있어도 안전, 없으면 더 깔끔" 한 형태로 작성됨. 시드와의 순서 의존이 없는 게 좋은 마이그레이션의 신호.

---

## 🔥 Testcontainers — 마이그레이션 자체가 테스트로 검증된다

이 SQL 이 문법·순서 모두 옳다는 걸 어떻게 아는가? **통합 테스트가 매번 증명**한다.

```
SdMmIntegrationTest 실행
   ↓
Testcontainers 가 빈 MySQL 8 컨테이너를 띄움
   ↓
Flyway 가 V1 → V2 → ... → V21 → V22 를 순서대로 전부 적용  ⭐
   ↓
(어느 한 줄이라도 문법 오류 / 순서 의존 깨짐 / FK 대상 없음이면 여기서 실패)
   ↓
적용 성공해야 비로소 테스트 본문(출하→재고 차감) 실행
```

즉 **전체 테스트가 통과했다는 사실 = V1~V22 의 SQL 이 빈 DB 에 순서대로 전부 적용된다는 증거**. V22 의 3단계가 빈 `delivery` 테이블에 대해서도 (backfill no-op 포함) 깨끗이 돈다는 것까지 자동 검증된다.

> 💡 마이그레이션을 손으로 검증할 필요가 거의 없는 이유 — CI 가 매번 `docker-compose down -v` 한 것과 동일한 "백지 MySQL" 에 전체 마이그레이션을 재생한다. Phase 3 워크스루 06편의 "수동 검증" 은 보조 수단이고, 1차 방어선은 Testcontainers 다.

---

## 🔥 두 컬럼이 만드는 양방향 연결

V21+V22 의 결과로 SD 와 MM 이 양쪽에서 서로를 가리킨다:

```
delivery                          goods_issue
┌─────────────────┐               ┌─────────────────┐
│ id              │◀──────────────│ delivery_id (NULL 가능)  V21
│ warehouse_id ───┼──┐            │ warehouse_id    │
└─────────────────┘  │            └─────────────────┘
                     │  V22
                     ▼
                 warehouse (WH-HQ ...)
```

- **V21** `goods_issue.delivery_id` → "이 출고는 어느 출하에서 자동 생성됐나" (MM → SD, nullable).
- **V22** `delivery.warehouse_id` → "이 출하는 어느 창고에서 나가나" (SD → MM, NOT NULL).

두 컬럼이 함께 SD↔MM 의 데이터 다리를 놓는다. 코드(JPA)는 둘 다 `Long` 약한 참조로만 들고 모듈 경계를 지키되, **무결성은 DB FK 두 개**(`fk_goods_issue_delivery`, `fk_delivery_warehouse`)가 책임진다.

---

## 자기 점검

- [ ] 빈 테이블 `CREATE` 와 기존 테이블 `ALTER` 의 본질적 차이는 무엇인가?
- [ ] V21 의 `delivery_id` 가 nullable 인 이유 — 어떤 두 출처가 한 테이블에 공존하는가?
- [ ] V22 의 3단계(nullable ADD → backfill → NOT NULL)에서 한 단계라도 건너뛰면 각각 무슨 일이 생기는가?
- [ ] backfill `UPDATE` 의 서브쿼리가 NULL 을 반환하지 않음을 무엇이 보장하는가?
- [ ] V14/V20 시드 덕분에 두 ALTER 가 "충돌 없음" 이 되는 이유는? backfill 이 no-op 인 까닭은?
- [ ] "전체 테스트 통과 = V21/V22 SQL 이 옳다" 가 성립하는 메커니즘은 무엇인가?

---

이전 편 → [04-약한-FK-와-Delivery-warehouse.md](./04-약한-FK-와-Delivery-warehouse.md)
다음 편 → [06-통합-시나리오-테스트.md](./06-통합-시나리오-테스트.md)
