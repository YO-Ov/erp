# 6/7. Flyway V15~V20 — 마이그레이션 6편의 의도

> 한 마이그레이션 = 한 단위, 시드는 마지막에 몰아넣기, CHECK 제약과 약한 참조.
> 설계서 §9 + Phase 1/2 의 마이그레이션 정책 그대로 + MM 특유의 결정.

대상 파일:

```
hwlee-erp/src/main/resources/db/migration/
├─ V15__create_warehouse.sql
├─ V16__create_stock.sql
├─ V17__create_stock_movement.sql
├─ V18__create_goods_receipt.sql
├─ V19__create_goods_issue.sql
└─ V20__seed_mm_demo.sql
```

---

## 🔥 분리 원칙 — 한 파일 = 한 단위

| 버전 | 단위 | 내용 |
| --- | --- | --- |
| V15 | warehouse | 마스터 1테이블 |
| V16 | stock | 캐시 1테이블 (CHECK + version) |
| V17 | stock_movement | 원장 1테이블 (인덱스 2개 + CHECK) |
| V18 | goods_receipt | 헤더 + 라인 2테이블 (한 트랜잭션 도메인) |
| V19 | goods_issue | 헤더 + 라인 2테이블 (한 트랜잭션 도메인) |
| V20 | seed | 시연용 시드 한 파일 |

→ Phase 1 / Phase 2 와 같은 정책:
- "헤더-라인" 은 한 마이그레이션 (분리하면 어색).
- 그 외는 1테이블 1마이그레이션.
- 시드는 무조건 마지막 한 파일에 몰아넣음.

장점:
- **롤백 단위가 명확** — V18 만 되돌리면 입고만 제거.
- **Git history 가 의미를 가짐** — `V16__create_stock.sql` 만 본 사람이 "재고 캐시" 라는 한 결정의 도입을 이해.
- **테스트 환경 재현** — Testcontainers 가 V1 부터 V20 까지 순차 실행해 상태 재현.

---

## 🔥 V15 — Warehouse 마스터

```sql
CREATE TABLE warehouse (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(30)  NOT NULL,
    name       VARCHAR(200) NOT NULL                COMMENT '창고명 (본사창고/지방창고 등)',
    address    VARCHAR(500)                         COMMENT '단일 필드 (구조화는 학습 범위 밖)',
    status     VARCHAR(16)  NOT NULL                COMMENT 'ACTIVE/INACTIVE/BLOCKED',
    created_at DATETIME     NOT NULL,
    created_by VARCHAR(64)  NOT NULL,
    updated_at DATETIME     NOT NULL,
    updated_by VARCHAR(64)  NOT NULL,
    deleted_at DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouse_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='창고 마스터 (재고가 실제로 있는 장소)';
```

Phase 1 의 `department` 와 거의 같은 모양:
- `code` UNIQUE (수동 입력, `WH-XXX` 패턴은 DTO Bean Validation 으로 강제).
- Audit 4컬럼 + Soft Delete (`deleted_at`).
- 컬럼 주석에 도메인 의도 — "왜 단일 필드인가" 같은 결정의 흔적.

특이점 없음. Phase 1 패턴의 깨끗한 재사용.

---

## 🔥 V16 — Stock 캐시의 3가지 결정

```sql
CREATE TABLE stock (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    item_id       BIGINT          NOT NULL,
    warehouse_id  BIGINT          NOT NULL,
    qty_on_hand   DECIMAL(18, 4)  NOT NULL DEFAULT 0    COMMENT '현재 보유 (캐시)',
    average_cost  DECIMAL(15, 2)  NOT NULL DEFAULT 0    COMMENT '이동평균 원가',
    version       BIGINT          NOT NULL DEFAULT 0    COMMENT 'JPA @Version 낙관적 락',
    created_at    DATETIME        NOT NULL,
    created_by    VARCHAR(64)     NOT NULL,
    updated_at    DATETIME        NOT NULL,
    updated_by    VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_item_warehouse (item_id, warehouse_id),
    KEY idx_stock_warehouse (warehouse_id),
    CONSTRAINT fk_stock_item      FOREIGN KEY (item_id)      REFERENCES item(id),
    CONSTRAINT fk_stock_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    CONSTRAINT chk_stock_qty_non_negative CHECK (qty_on_hand >= 0),
    CONSTRAINT chk_stock_avg_non_negative CHECK (average_cost >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='재고 캐시 ((상품, 창고)당 보유량과 평균 원가)';
```

3가지 결정의 SQL 표현:

### 1. `UNIQUE (item_id, warehouse_id)` — (상품, 창고) 당 1행 보장

DB 차원에서 강제. 도메인 메서드(`Stock.empty`)가 같은 조합을 두 번 만들려고 해도 `DataIntegrityViolationException`. 동시 INSERT race 의 안전망 (낙관 락 편 §4 의 `orElseGet` 패턴).

### 2. `CHECK (qty_on_hand >= 0)` — 음수 재고의 마지막 보루

도메인 `Stock.issue` 가 가용 검증을 하지만, **도메인을 우회하는 경로** (직접 SQL 조작, 코드 버그 등) 에서 마지막 방어선. 설계서 §0 #8 "두 겹 방어" 의 구현.

MySQL 8 부터 CHECK 제약이 enforce 됨 (5.7 이전엔 무시). docker-compose 의 `mysql:8.0` 이면 작동.

### 3. `version BIGINT NOT NULL DEFAULT 0` — JPA @Version 의 컬럼

DEFAULT 0 — 첫 INSERT 시 명시 안 하면 0 으로 시작. JPA 가 Stock.empty() 새 행 만들 때 version 을 null 로 두면 JPA 가 0 으로 채움 (BIGINT 기본값).

V20 시드의 직접 INSERT 도 `version=0` 으로 시작.

---

## 🔥 V17 — StockMovement 원장 + 인덱스 2개

```sql
CREATE TABLE stock_movement (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    item_id       BIGINT          NOT NULL,
    warehouse_id  BIGINT          NOT NULL,
    qty_delta     DECIMAL(18, 4)  NOT NULL                COMMENT '+ 입고 / - 출고',
    unit_cost     DECIMAL(15, 2)  NOT NULL                COMMENT '이 이동의 단가 (입고:매입단가, 출고:직전 평균)',
    reason        VARCHAR(20)     NOT NULL                COMMENT 'GOODS_RECEIPT/GOODS_ISSUE/ADJUSTMENT_PLUS/ADJUSTMENT_MINUS/SCRAP',
    ref_type      VARCHAR(10)                             COMMENT 'GR/GI/ADJ (선택)',
    ref_id        BIGINT                                  COMMENT '트랜잭션 ID (약한 참조)',
    moved_at      DATETIME        NOT NULL                COMMENT '이동 일시',
    created_at    DATETIME        NOT NULL,
    created_by    VARCHAR(64)     NOT NULL,
    updated_at    DATETIME        NOT NULL,
    updated_by    VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_stock_movement_item_wh_time (item_id, warehouse_id, moved_at),
    KEY idx_stock_movement_ref (ref_type, ref_id),
    CONSTRAINT fk_stock_movement_item      FOREIGN KEY (item_id)      REFERENCES item(id),
    CONSTRAINT fk_stock_movement_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    CONSTRAINT chk_stock_movement_delta_nonzero CHECK (qty_delta <> 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='재고 이동 원장 (append-only)';
```

3가지 주목:

### 인덱스 1: `(item_id, warehouse_id, moved_at)`

가장 흔한 쿼리 — "이 상품 이 창고의 시간 순 이력":
```sql
SELECT * FROM stock_movement
 WHERE item_id = ? AND warehouse_id = ?
   AND moved_at >= ?
 ORDER BY moved_at DESC
```

복합 인덱스의 **컬럼 순서가 핵심**. `item_id` 가 가장 선택적 (item 1000개) → `warehouse_id` (10개) → `moved_at` (시간 정렬). 좌측부터 좁혀가는 게 가장 효율적.

> 💡 MySQL InnoDB 의 복합 인덱스는 좌측 prefix 만 사용 가능. `WHERE warehouse_id = ?` 만 있으면 이 인덱스 안 탐 → `idx_stock_warehouse` 같은 보조 인덱스가 필요할 수도. Phase 3 은 "상품-창고-시간" 쿼리가 주력이라 한 인덱스로 충분.

### 인덱스 2: `(ref_type, ref_id)`

역추적 — "GR-42 가 만든 이동 행 모두":
```sql
SELECT * FROM stock_movement
 WHERE ref_type = 'GR' AND ref_id = 42
```

입고 취소 시 같은 GR id 로 음수 ADJUSTMENT 행을 찾아 보정하는 패턴. 또는 디버깅용 — "이 출고 트랜잭션이 정확히 어떤 재고 변동을 일으켰나" 추적.

### `CHECK (qty_delta <> 0)` — 의미 없는 행 차단

`qty_delta = 0` 인 이동은 정보 0. DB 차원에서 거부. 도메인 `StockMovement.of()` 도 똑같이 검증하지만 (`qtyDelta.signum() == 0` → IllegalArgumentException), DB 가 마지막 보루.

### `ref_type / ref_id` FK 가 아닌 이유 (다시)

위 워크스루 02편 §ref_type 에서 다룬 약한 참조. 새 이동 유형이 추가될 때 컬럼 안 늘리기 위함.

---

## 🔥 V18 — GoodsReceipt 헤더 + 라인

```sql
CREATE TABLE goods_receipt (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    number        VARCHAR(30)     NOT NULL                COMMENT '예: GR-20260528-001',
    vendor_id     BIGINT          NOT NULL                COMMENT '매입처',
    warehouse_id  BIGINT          NOT NULL                COMMENT '받는 창고',
    status        VARCHAR(16)     NOT NULL                COMMENT 'DRAFT/POSTED/CANCELLED',
    receipt_date  DATE            NOT NULL,
    posted_at     DATETIME                                COMMENT '확정 시각 (감사용)',
    created_at    DATETIME        NOT NULL,
    created_by    VARCHAR(64)     NOT NULL,
    updated_at    DATETIME        NOT NULL,
    updated_by    VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_goods_receipt_number (number),
    KEY idx_goods_receipt_vendor (vendor_id),
    KEY idx_goods_receipt_warehouse (warehouse_id),
    KEY idx_goods_receipt_date (receipt_date),
    CONSTRAINT fk_goods_receipt_vendor    FOREIGN KEY (vendor_id)    REFERENCES vendor(id),
    CONSTRAINT fk_goods_receipt_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='입고 헤더';

CREATE TABLE goods_receipt_line (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    goods_receipt_id  BIGINT          NOT NULL,
    line_no           INT             NOT NULL,
    item_id           BIGINT          NOT NULL,
    quantity          DECIMAL(18, 4)  NOT NULL,
    unit_cost         DECIMAL(15, 2)  NOT NULL                COMMENT '이 입고의 단가',
    line_total        DECIMAL(15, 2)  NOT NULL                COMMENT 'qty × unit_cost',
    created_at        DATETIME        NOT NULL,
    created_by        VARCHAR(64)     NOT NULL,
    updated_at        DATETIME        NOT NULL,
    updated_by        VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_goods_receipt_line_header (goods_receipt_id),
    KEY idx_goods_receipt_line_item (item_id),
    CONSTRAINT fk_goods_receipt_line_header FOREIGN KEY (goods_receipt_id) REFERENCES goods_receipt(id),
    CONSTRAINT fk_goods_receipt_line_item   FOREIGN KEY (item_id)          REFERENCES item(id),
    CONSTRAINT chk_goods_receipt_line_qty CHECK (quantity > 0),
    CONSTRAINT chk_goods_receipt_line_cost CHECK (unit_cost >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='입고 라인';
```

Phase 2 의 `sales_order` / `sales_order_line` 과 같은 패턴:
- 헤더 `number UNIQUE` — 일자 단위 시퀀스.
- 헤더 ↔ 라인 FK (`goods_receipt_id`).
- 라인 → 마스터 FK (`item_id` → item).

차이점:
- 헤더에 `vendor_id` FK — 입고는 누구한테 받았는지가 의미 있음.
- 라인에 `unit_cost` 와 `line_total` — Phase 2 SO 의 `unit_price` 와 같은 자리지만 매입 측이라 `cost`.

### `chk_goods_receipt_line_qty CHECK (quantity > 0)`, `chk_goods_receipt_line_cost CHECK (unit_cost >= 0)`

도메인 (`GoodsReceiptLine` 생성자) 도 같은 검증. **두 겹 방어** — Phase 2 의 SO 라인은 이런 DB CHECK 가 없었는데, MM 부터 도입. 데이터 무결성의 본질이 더 강한 영역이라.

---

## 🔥 V19 — GoodsIssue 의 다른 모양

```sql
CREATE TABLE goods_issue (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    number        VARCHAR(30)     NOT NULL                COMMENT '예: GI-20260528-001',
    warehouse_id  BIGINT          NOT NULL                COMMENT '나가는 창고',
    status        VARCHAR(16)     NOT NULL                COMMENT 'DRAFT/POSTED/CANCELLED',
    issue_date    DATE            NOT NULL,
    reason        VARCHAR(20)     NOT NULL                COMMENT 'SHIPMENT/ADJUSTMENT/SCRAP',
    posted_at     DATETIME                                COMMENT '확정 시각 (감사용)',
    created_at    DATETIME        NOT NULL,
    created_by    VARCHAR(64)     NOT NULL,
    updated_at    DATETIME        NOT NULL,
    updated_by    VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_goods_issue_number (number),
    KEY idx_goods_issue_warehouse (warehouse_id),
    KEY idx_goods_issue_date (issue_date),
    CONSTRAINT fk_goods_issue_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='출고 헤더';

CREATE TABLE goods_issue_line (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    goods_issue_id  BIGINT          NOT NULL,
    line_no         INT             NOT NULL,
    item_id         BIGINT          NOT NULL,
    quantity        DECIMAL(18, 4)  NOT NULL,
    created_at      DATETIME        NOT NULL,
    created_by      VARCHAR(64)     NOT NULL,
    updated_at      DATETIME        NOT NULL,
    updated_by      VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_goods_issue_line_header (goods_issue_id),
    KEY idx_goods_issue_line_item (item_id),
    CONSTRAINT fk_goods_issue_line_header FOREIGN KEY (goods_issue_id) REFERENCES goods_issue(id),
    CONSTRAINT fk_goods_issue_line_item   FOREIGN KEY (item_id)        REFERENCES item(id),
    CONSTRAINT chk_goods_issue_line_qty CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='출고 라인';
```

GR 와의 차이:
- **헤더에 `vendor_id` 없음** — 출고는 "어디로 보냈는가" 가 다양 (Phase 4 에서 Customer/SO 정보가 자동 연결).
- **헤더에 `reason`** — 출고 사유 (SHIPMENT/ADJUSTMENT/SCRAP).
- **라인에 `unit_cost` / `line_total` 없음** — 출고 단가는 Stock 평균을 적용해 StockMovement 에 박힘. GoodsIssueLine 은 수량만 들고 있음.

→ 입출고 두 도메인이 거의 대칭이지만, 비즈니스 의미의 차이가 작은 컬럼 차이로 나타남. 차이를 한 줄 한 줄 알아보는 것이 학습.

---

## 🔥 V20 — 시드의 함정과 우회

```sql
-- 1) 본사창고 (Department 식 명시적 코드)
INSERT INTO warehouse (code, name, address, status, created_at, created_by, updated_at, updated_by, deleted_at)
VALUES ('WH-HQ', '본사창고', '서울시 강남구', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL);

SET @wh_id = LAST_INSERT_ID();

-- 2) 초기 입고 헤더 (POSTED 로 직접 시드)
INSERT INTO goods_receipt (number, vendor_id, warehouse_id, status, receipt_date, posted_at,
                           created_at, created_by, updated_at, updated_by)
SELECT CONCAT('GR-', DATE_FORMAT(NOW(), '%Y%m%d'), '-001'),
       v.id, @wh_id, 'POSTED', CURDATE(), NOW(),
       NOW(), 'system', NOW(), 'system'
  FROM vendor v WHERE v.code = CONCAT('VEND-', YEAR(NOW()), '-0001');

SET @gr_id = LAST_INSERT_ID();

-- 3) 입고 라인 — 노트북 50대 @ 100만원
INSERT INTO goods_receipt_line (goods_receipt_id, line_no, item_id, quantity, unit_cost, line_total, ...)
SELECT @gr_id, 1, i.id, 50.0000, 1000000.00, 50000000.00, ...
  FROM item i WHERE i.code = CONCAT('ITEM-', YEAR(NOW()), '-0001');

-- 4) Stock 캐시 행 — qty=50, avg=100만, version=0
INSERT INTO stock (item_id, warehouse_id, qty_on_hand, average_cost, version, ...)
SELECT i.id, @wh_id, 50.0000, 1000000.00, 0, ...
  FROM item i WHERE i.code = CONCAT('ITEM-', YEAR(NOW()), '-0001');

-- 5) 원장에 +50 적재
INSERT INTO stock_movement (item_id, warehouse_id, qty_delta, unit_cost, reason, ref_type, ref_id, moved_at, ...)
SELECT i.id, @wh_id, 50.0000, 1000000.00, 'GOODS_RECEIPT', 'GR', @gr_id, NOW(), ...
  FROM item i WHERE i.code = CONCAT('ITEM-', YEAR(NOW()), '-0001');

-- 6) 트랜잭션 시퀀스 초기화 — 오늘 날짜로 GR 001 사용했으므로 next_number=2
INSERT INTO code_sequence (prefix, period_key, next_number, updated_at) VALUES
    ('GR', DATE_FORMAT(NOW(), '%Y%m%d'), 2, NOW());
```

### 함정: 시드는 도메인 메서드를 부를 수 없다

`GoodsReceiptService.post(grId)` 한 줄로 끝낼 수 있는 일을 SQL 로 풀어 헤치는 이유 — Flyway 는 자바 코드가 아니라 SQL. `GoodsReceipt.post()` 도메인 메서드와 `Stock.receive()`, `StockMovement.of()` 의 순서를 모두 직접 흉내내야 한다.

5단계 INSERT 가 한 트랜잭션 안에서:
1. `warehouse` — WH-HQ 만들고 `@wh_id` 저장.
2. `goods_receipt` — POSTED 상태로 직접. `@gr_id` 저장.
3. `goods_receipt_line` — qty=50, unit_cost=100만, line_total=5000만.
4. `stock` — qty=50, avg=100만, version=0.
5. `stock_movement` — +50, unit_cost=100만, reason=GOODS_RECEIPT, ref=`@gr_id`.

→ 도메인 메서드의 효과를 SQL 로 정확히 재현. 한 줄이라도 빠지면 통합 테스트가 깨진다 (`MmScenarioTest.원장_합계는_Stock_과_일치한다` 시드 단계부터 검증).

### 6단계 시퀀스 초기화

V14 (Phase 2) 와 같은 패턴 — 시드로 사용한 번호가 정확히 어디까지인지 `code_sequence` 에 적어둠. 그러지 않으면:
- 첫 시연 API 호출 (`POST /api/goods-receipts`) 이 `nextGoodsReceiptNumber(...)` 호출.
- `code_sequence` 에 GR/오늘 행이 없으니 next_number=1 부터 시작.
- 결과: `GR-YYYYMMDD-001` — **이미 시드로 사용한 번호와 충돌**! UNIQUE 위반.

→ 시드의 마지막은 항상 "내가 어디까지 썼는지 알려주는 행" 으로 마무리.

### `DATE_FORMAT(NOW(), '%Y%m%d')` 의 의미

`GR-20260528-001` 의 일자 부분을 **마이그레이션이 실행되는 날짜** 로 동적 생성. 새 PC 에서 셋업하면 그날 날짜로 시드됨.

→ "오늘 기준" 시연 데이터를 매번 만들기 위한 트릭. V8/V14 와 같은 방침.

---

## 🔥 시드의 다른 옵션과 비교

대안 1: **시드를 자바 코드로** (`@PostConstruct` 또는 `CommandLineRunner`)
- 장점: 도메인 메서드 그대로 호출. `goodsReceiptService.post(...)` 한 줄.
- 단점: 환경(local/test) 분기 어려움, 멱등성 보장 따로 만들어야, Flyway 와 책임 중복.

대안 2: **시드 없음** — 시연 시 사용자가 직접 API 호출로 만들기
- 장점: 의존 0.
- 단점: 시연 시작 전 매번 동일한 데이터 셋팅 → 학습 시간 낭비.

대안 3: **시드를 SQL 로** (우리 채택)
- 장점: Flyway 가 환경별로 알아서 실행, idempotent (한 번만 적용), Testcontainers 도 자동 적용.
- 단점: 도메인 메서드 우회. 일관성을 SQL 로 직접 맞춰야 함.

→ "운영용이 아닌 학습/시연 보조용" 의 시드라 3번이 적절. 5단계 INSERT 가 도메인의 흐름을 보여주는 학습 효과도 있음.

---

## 🔥 Phase 1/2 와의 누적성

```
V1  — init (Flyway 메타테이블)
V2  — code_sequence
V3~V7 — 마스터 5개
V8  — Phase 1 시드
V9  — code_sequence.year → period_key
V10~V13 — SD 4개 트랜잭션
V14 — Phase 2 시드
V15  — warehouse
V16~V17 — Stock + Movement (캐시 + 원장)
V18~V19 — GR/GI
V20 — Phase 3 시드
```

각 Phase 가 5~7개 마이그레이션을 차곡차곡 추가. Phase 3 끝나면 V20. Phase 4 (SD↔MM 연계) 는 새 테이블 없이 코드만 추가하면 끝날 가능성도 있음.

**리뷰 시점**: 마이그레이션 번호가 마지막 한 자리수 가까이 가면 (V99 → V100), 100단위 jump 도 흔한 컨벤션. 현재는 한참 멀음.

---

## 🔥 마이그레이션 검증 (수동)

```bash
# 마이그레이션 적용 후 결과 확인
docker exec -it erp-mysql mysql -uerp -perp erp_db -e "
  SHOW TABLES;
  SELECT prefix, period_key, next_number FROM code_sequence ORDER BY prefix;
  SELECT code, name FROM warehouse;
  SELECT s.id, i.code AS item, w.code AS warehouse, s.qty_on_hand, s.average_cost, s.version
    FROM stock s
    JOIN item i ON i.id = s.item_id
    JOIN warehouse w ON w.id = s.warehouse_id;
  SELECT id, qty_delta, unit_cost, reason, ref_type, ref_id FROM stock_movement;
"
```

기대 출력 (V20 직후):
```
TABLES: 16개 (customer, ... warehouse, stock, stock_movement, goods_receipt, goods_receipt_line, goods_issue, goods_issue_line, ...)
code_sequence: GR 오늘날짜 next=2, ... (다른 prefix 들도)
warehouse: 1행 — WH-HQ 본사창고
stock: 1행 — 노트북 본사창고 qty=50, avg=1000000, version=0
stock_movement: 1행 — +50, 1000000, GOODS_RECEIPT, GR, <gr_id>
```

각 행이 정합성 식 만족: `SUM(stock_movement.qty_delta) WHERE (item, warehouse) = stock.qty_on_hand`
→ `50 == 50` ✓

---

## 🔥 마이그레이션이 깨졌을 때

`./gradlew bootRun` 또는 통합 테스트 시 Flyway 가 V15~V20 를 순차 적용. 어느 단계가 깨지면:

```
Migration V18__create_goods_receipt.sql failed
SQLException: Cannot add foreign key constraint
```

해석: V18 의 `fk_goods_receipt_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(id)` 가 거부. 원인:
- vendor 테이블 컬럼 타입 불일치 (BIGINT vs INT) — Phase 1 V5 확인.
- 또는 vendor 테이블 자체가 없음 — V5 마이그레이션 실패한 적이 있는지.

**중요**: Flyway 가 실패하면 **그 마이그레이션 이전까지만 적용된 상태로 남는다**. 절대 자동 롤백 안 함 (그러면 다음 시도가 V18 부터 — 이미 부분 적용되었을 수도). 운영에선 손으로 정리해야 하지만 학습 환경은:

```bash
# 깨끗하게 초기화 후 재시도
docker-compose down -v
docker-compose up -d
./gradlew bootRun
```

---

## 자기 점검

- [ ] 한 마이그레이션 = 한 단위 원칙 — V18 이 헤더+라인 2테이블을 한 파일에 두는 이유는?
- [ ] `CHECK (qty_on_hand >= 0)` 가 도메인 검증과 중복인데 왜 두는가?
- [ ] V17 의 두 인덱스가 노리는 쿼리 패턴은 각각 무엇?
- [ ] V20 시드가 5단계 INSERT 를 하나하나 직접 하는 이유는?
- [ ] V20 마지막의 `code_sequence` INSERT 가 없으면 어떤 일이 일어나는가?
- [ ] `DATE_FORMAT(NOW(), '%Y%m%d')` 를 시드에 박는 이유는?

---

이전 편 → [05-이동평균-원가.md](./05-이동평균-원가.md)
다음 편 → [07-동시성-테스트.md](./07-동시성-테스트.md)
