# 6/7. Flyway 마이그레이션 V9 ~ V14

> Phase 2 의 모든 DDL + 트랜잭션 시드. 자바 엔티티가 어떤 SQL DDL 로 표현되는지 양쪽을 비교.

대상 파일:

```
hwlee-erp/src/main/resources/db/migration/
├─ V9__alter_code_sequence_period_key.sql   (Phase 1 테이블 수정)
├─ V10__create_quotation.sql                (견적 헤더 + 라인)
├─ V11__create_sales_order.sql              (수주 헤더 + 라인)
├─ V12__create_delivery.sql                 (출하 헤더 + 라인)
├─ V13__create_invoice.sql                  (인보이스 헤더 + 라인)
└─ V14__seed_sd_demo.sql                    (시연용 DRAFT 견적/수주)
```

설계서 §8 마이그레이션 계획의 코드 버전.

---

## 🔥 V9 — Phase 1 이후 첫 destructive change

```sql
ALTER TABLE code_sequence
    CHANGE COLUMN year period_key VARCHAR(8) NOT NULL
        COMMENT '발급 단위 — 마스터: YYYY, 트랜잭션: YYYYMMDD';

ALTER TABLE code_sequence
    DROP INDEX uk_code_sequence_prefix_year,
    ADD UNIQUE KEY uk_code_sequence_prefix_period (prefix, period_key);
```

01편 시퀀스 확장에서 자세히 다룬 마이그레이션. 핵심 포인트만 다시 짚으면:

### 한 마이그레이션 = 한 단위 원칙의 예외

Phase 1 워크스루 08편에서 "한 파일 = 한 테이블" 원칙을 말했다. V9 도 그 원칙을 따르지만, **타입 변경 + 인덱스 변경** 두 작업을 한 파일에 묶었다 — 두 작업이 **하나의 논리적 단위** (컬럼 일반화)이기 때문.

### 데이터 보존
- V8 시드의 `year=2026` (INT) → V9 `CHANGE COLUMN` 후 `period_key='2026'` (VARCHAR)
- MySQL 의 자동 캐스팅이 자연스럽게 처리.

V8 파일 자체는 손대지 않는다. **Flyway 가 V8 의 체크섬을 검증**하므로, 한 번 적용된 마이그레이션 수정 = 운영에서 거부.

---

## 🔥 V10 — Quotation 헤더 + 라인

```sql
CREATE TABLE quotation (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    number        VARCHAR(30)     NOT NULL                COMMENT '예: Q-20260524-001',
    customer_id   BIGINT          NOT NULL,
    status        VARCHAR(16)     NOT NULL                COMMENT 'DRAFT/SENT/ACCEPTED/EXPIRED/CANCELLED',
    issued_date   DATE            NOT NULL                COMMENT '발행일',
    valid_until   DATE                                    COMMENT '유효 기한 (NULL=미지정)',
    total_amount  DECIMAL(15, 2)  NOT NULL DEFAULT 0,
    created_at    DATETIME        NOT NULL,
    created_by    VARCHAR(64)     NOT NULL,
    updated_at    DATETIME        NOT NULL,
    updated_by    VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_quotation_number (number),
    KEY idx_quotation_customer (customer_id),
    KEY idx_quotation_issued_date (issued_date),
    CONSTRAINT fk_quotation_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='견적 헤더';

CREATE TABLE quotation_line (
    ...
    CONSTRAINT fk_quotation_line_quotation FOREIGN KEY (quotation_id) REFERENCES quotation(id),
    CONSTRAINT fk_quotation_line_item FOREIGN KEY (item_id) REFERENCES item(id)
) ENGINE=InnoDB ...;
```

### 트랜잭션 테이블에 `deleted_at` 이 없다

Phase 1 의 `customer` 와 비교:

```sql
-- V3 customer 의 컬럼
created_at    DATETIME    NOT NULL,
created_by    VARCHAR(64) NOT NULL,
updated_at    DATETIME    NOT NULL,
updated_by    VARCHAR(64) NOT NULL,
deleted_at    DATETIME,    ← Soft Delete 표시
```

```sql
-- V10 quotation 의 컬럼
created_at    DATETIME    NOT NULL,
created_by    VARCHAR(64) NOT NULL,
updated_at    DATETIME    NOT NULL,
updated_by    VARCHAR(64) NOT NULL,
                          ← deleted_at 없음
```

02편에서 짚었듯이 **트랜잭션은 물리 삭제 안 하고 `CANCELLED` 상태로 표현**. `BaseEntityWithCode` 가 아니라 `BaseEntity` 만 상속하니 컬럼 자체가 없다.

### 인덱스 결정

```sql
PRIMARY KEY (id),
UNIQUE KEY uk_quotation_number (number),          -- 단건 조회 + 발급 중복 방지
KEY idx_quotation_customer (customer_id),         -- "이 고객의 견적 목록"
KEY idx_quotation_issued_date (issued_date),      -- 날짜 범위 조회
```

- `number` UNIQUE: 발급 중복 안전망 (`CodeGenerator` 가 1차 방어, DB 가 2차).
- `customer_id` 인덱스: 영업 화면 "신원전자 견적 목록" 같은 조회.
- `issued_date` 인덱스: "이번 달 견적" 같은 범위 조회. 회계 마감의 기준일.
- `status` 인덱스: 카디널리티 낮아 (5개 값) 인덱스 효과 낮음 → 생략. 필요 시 Phase 후반에 보강.

### 라인 테이블의 인덱스

```sql
KEY idx_quotation_line_quotation (quotation_id),
KEY idx_quotation_line_item (item_id),
```

`quotation_id` 인덱스가 필수 — `WHERE quotation_id = ?` 가 가장 빈번 (라인 컬렉션 페치).
`item_id` 인덱스는 "이 상품이 들어간 견적 라인" 같은 역방향 조회 용. Phase 2 에서는 자주 안 쓰지만 미래의 리포트 (`상품별 판매량 집계`) 를 위해.

### `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`

Phase 1 의 모든 테이블에 동일. `InnoDB` 가 트랜잭션 + FK 를 지원하는 유일한 MySQL 엔진. `utf8mb4` 가 한국어 + 이모지 + 4바이트 문자까지 안전.

---

## 🔥 V11 — SalesOrder (3개 FK 가 있는 첫 테이블)

```sql
CREATE TABLE sales_order (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    number          VARCHAR(30)     NOT NULL,
    customer_id     BIGINT          NOT NULL,
    salesperson_id  BIGINT                                  COMMENT 'Employee (담당자, 선택)',
    quotation_id    BIGINT                                  COMMENT '견적에서 유래한 경우 (선택)',
    status          VARCHAR(16)     NOT NULL,
    order_date      DATE            NOT NULL,
    confirmed_at    DATETIME                                COMMENT '확정 시각 (감사용)',
    total_amount    DECIMAL(15, 2)  NOT NULL DEFAULT 0,
    created_at    DATETIME        NOT NULL,
    ...
    PRIMARY KEY (id),
    UNIQUE KEY uk_sales_order_number (number),
    KEY idx_sales_order_customer (customer_id),
    KEY idx_sales_order_status (status),
    KEY idx_sales_order_order_date (order_date),
    CONSTRAINT fk_sales_order_customer    FOREIGN KEY (customer_id)    REFERENCES customer(id),
    CONSTRAINT fk_sales_order_salesperson FOREIGN KEY (salesperson_id) REFERENCES employee(id),
    CONSTRAINT fk_sales_order_quotation   FOREIGN KEY (quotation_id)   REFERENCES quotation(id)
) ENGINE=InnoDB ...;
```

### nullable FK 두 개

```sql
salesperson_id  BIGINT          -- NOT NULL 없음 → nullable
quotation_id    BIGINT          -- 동일
```

- 담당자 미지정 수주: 일괄 등록 / 자동 등록 시나리오.
- 견적 없이 직행: 견적 → 수주 전환이 아닌 신규 직발주.

→ NULL 허용 = "이 정보가 없어도 수주는 성립한다" 라는 도메인 결정. NOT NULL 로 잠그면 영업 입장에서 운영이 빡빡해진다.

### `status` 인덱스가 V10 과 다른 이유

V10 (quotation) 은 status 인덱스를 안 만들었는데 V11 (sales_order) 은 만들었다. 이유:
- 수주의 핵심 운영 쿼리: **"CONFIRMED 이상의 활성 수주 합계"** (신용한도 검증, 05편).
- `WHERE status IN ('CONFIRMED', 'SHIPPING', ...)` 가 자주 발사됨.
- 카디널리티는 낮지만 (8개) 호출 빈도가 높다 → 인덱스 효과.

### `order_date` 인덱스

회계 마감(`WHERE order_date BETWEEN ... AND ...`) 의 기준일. **트랜잭션의 운영 쿼리는 대부분 날짜 범위가 들어간다** 는 패턴.

### 라인 테이블의 누적 컬럼

```sql
CREATE TABLE sales_order_line (
    ...
    order_qty       DECIMAL(15, 4)  NOT NULL                COMMENT '주문량 (확정 후 불변)',
    shipped_qty     DECIMAL(15, 4)  NOT NULL DEFAULT 0      COMMENT '출하 누적',
    invoiced_qty    DECIMAL(15, 4)  NOT NULL DEFAULT 0      COMMENT '청구 누적',
    unit_price      DECIMAL(15, 2)  NOT NULL,
    line_total      DECIMAL(15, 2)  NOT NULL                COMMENT 'order_qty * unit_price (공급가)',
    ...
);
```

`shipped_qty / invoiced_qty` 의 `DEFAULT 0` 가 중요:
- INSERT 시 명시 안 해도 0 — 새 라인이 만들어지자마자 누적은 0.
- 자바 엔티티 (`SalesOrderLine`) 가 `BigDecimal.ZERO` 초기값 — DB 디폴트와 일치.

이중 안전망:
- 엔티티가 0 으로 초기화 → INSERT 시 0 전송.
- 만약 외부 SQL 로 INSERT 한다면 DEFAULT 0 이 받쳐줌.

### `DECIMAL(15, 4)` 가 4자리 소수

02편에서 짚었듯이 무게/길이 단위 상품을 위한 일반화.

---

## 🔥 V12 — Delivery (SO 라인 FK 가 핵심)

```sql
CREATE TABLE delivery (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    number          VARCHAR(30)     NOT NULL,
    sales_order_id  BIGINT          NOT NULL,
    status          VARCHAR(16)     NOT NULL,
    shipped_date    DATE            NOT NULL,
    ...
    CONSTRAINT fk_delivery_sales_order FOREIGN KEY (sales_order_id) REFERENCES sales_order(id)
);

CREATE TABLE delivery_line (
    id                    BIGINT          NOT NULL AUTO_INCREMENT,
    delivery_id           BIGINT          NOT NULL,
    sales_order_line_id   BIGINT          NOT NULL,           -- ← 핵심 FK
    line_no               INT             NOT NULL,
    quantity              DECIMAL(15, 4)  NOT NULL,
    ...
    CONSTRAINT fk_delivery_line_sol FOREIGN KEY (sales_order_line_id) REFERENCES sales_order_line(id)
);
```

설계서 §0 #8: **"Delivery/Invoice 라인은 `sales_order_line_id` 를 직접 FK 참조"**.

### `sales_order_id` 와 `sales_order_line_id` 두 단계 참조

- 헤더 → 헤더: `delivery.sales_order_id`
- 라인 → 라인: `delivery_line.sales_order_line_id`

`delivery_line.sales_order_line_id → sales_order_line.id` 만 있어도 `sales_order_line.sales_order_id` 로 거슬러 갈 수 있다. 그런데 `delivery.sales_order_id` 도 같이 두는 이유:
- **빠른 조회**: "이 수주의 출하 목록" 을 `SELECT FROM delivery WHERE sales_order_id = ?` 한 줄로. 라인 단계까지 안 가도 됨.
- **무결성**: 한 Delivery 의 모든 라인이 같은 수주에 속한다는 도메인 약속을 헤더에서 한 번 더 못 박음.

### `delivery_line` 에 `item_id` 가 없음

04편에서 짚었듯 — `sales_order_line` 이 이미 들고 있어서. DB 차원의 중복 제거.

---

## 🔥 V13 — Invoice (3개 금액 컬럼)

```sql
CREATE TABLE invoice (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    number          VARCHAR(30)     NOT NULL,
    sales_order_id  BIGINT          NOT NULL,
    status          VARCHAR(16)     NOT NULL,
    invoice_date    DATE            NOT NULL,
    subtotal        DECIMAL(15, 2)  NOT NULL DEFAULT 0      COMMENT '공급가액 (부가세 제외)',
    tax_amount      DECIMAL(15, 2)  NOT NULL DEFAULT 0      COMMENT '부가세 (subtotal × 0.10)',
    total_amount    DECIMAL(15, 2)  NOT NULL DEFAULT 0      COMMENT 'subtotal + tax_amount',
    ...
);

CREATE TABLE invoice_line (
    ...
    sales_order_line_id   BIGINT          NOT NULL,
    quantity              DECIMAL(15, 4)  NOT NULL,
    unit_price            DECIMAL(15, 2)  NOT NULL  COMMENT 'SO 라인 단가 복사 (가격 동결)',
    line_total            DECIMAL(15, 2)  NOT NULL  COMMENT 'qty × unit_price (공급가)',
    ...
);
```

### 3개 금액 컬럼 분리
04편에서 짚었듯 — 한국 회계 처리상 공급가/부가세 분리 의무 + 리포트 편의.

### `unit_price` 가 라인에 있다 (Delivery 와 다름)
출하 라인은 수량만, 인보이스 라인은 수량 + 단가. **회계 보존**을 위한 가격 동결.

---

## 🔥 V14 — 시연용 시드 (트랜잭션을 시드하는 예외적 결정)

```sql
-- 1) DRAFT 견적 1건 — 신원전자 대상, 노트북 5대 + 모니터 3대
INSERT INTO quotation (number, customer_id, status, issued_date, valid_until, total_amount,
                       created_at, created_by, updated_at, updated_by)
SELECT CONCAT('Q-', DATE_FORMAT(NOW(), '%Y%m%d'), '-001'),
       c.id, 'DRAFT', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY),
       7050000.00, NOW(), 'system', NOW(), 'system'
  FROM customer c WHERE c.code = CONCAT('CUST-', YEAR(NOW()), '-0001');

SET @q_id = LAST_INSERT_ID();

INSERT INTO quotation_line (...) SELECT @q_id, 1, ...;
INSERT INTO quotation_line (...) SELECT @q_id, 2, ...;

-- 2) DRAFT 수주 1건 — 같은 고객, 노트북 10대
INSERT INTO sales_order (...) SELECT ... ;
SET @so_id = LAST_INSERT_ID();
INSERT INTO sales_order_line (...) SELECT @so_id, 1, ...;

-- 3) 트랜잭션 시퀀스 초기화
INSERT INTO code_sequence (prefix, period_key, next_number, updated_at) VALUES
    ('Q',  DATE_FORMAT(NOW(), '%Y%m%d'), 2, NOW()),
    ('SO', DATE_FORMAT(NOW(), '%Y%m%d'), 2, NOW());
```

### "트랜잭션은 시드 안 한다" 원칙의 예외

Phase 1 V8 시드 헤더의 한 줄:

> 트랜잭션 데이터는 절대 시드하지 않음.

V14 가 이 원칙을 어긴다. 왜?

- **학습/시연**: 빈 DB 로 시연을 시작하면 화면에 보여줄 게 없다.
- **DRAFT 상태로만**: 확정/출하/인보이스는 시드하지 않는다 — 시연 흐름에서 직접 진행시킬 거리.

→ **시드 대상은 "초기 작업물" 까지만**. 운영 환경이라면 V14 는 빠진다 (Flyway profile 별 마이그레이션 위치 분리는 학습 범위 밖).

### `LAST_INSERT_ID()` 패턴

```sql
SET @q_id = LAST_INSERT_ID();
INSERT INTO quotation_line (...) SELECT @q_id, ...;
```

MySQL 의 세션 변수 `@q_id` 에 방금 만든 PK 를 담아 다음 INSERT 에서 참조. 이 패턴은 Phase 1 V8 에서도 사용 (`@hq_id` 부서 트리).

> ⚠️ 트랜잭션 시드를 멀티 스레드로 실행하면 `LAST_INSERT_ID()` 가 꼬일 수 있다. Flyway 는 직렬로 실행하므로 안전.

### 시퀀스 초기화

```sql
INSERT INTO code_sequence (prefix, period_key, next_number, updated_at) VALUES
    ('Q',  DATE_FORMAT(NOW(), '%Y%m%d'), 2, NOW()),
    ('SO', DATE_FORMAT(NOW(), '%Y%m%d'), 2, NOW());
```

위에서 `Q-YYYYMMDD-001`, `SO-YYYYMMDD-001` 을 직접 INSERT 했다. **다음 발급은 002 부터** 시작하도록 시퀀스 행을 미리 만든다 (`next_number = 2`).

이게 없으면:
1. 시드 후 첫 API 호출이 `nextTransactionCode("SO", "20260524")` 발급 시도.
2. `findForUpdate` → 행 없음 → `createOrLockExisting` → INSERT (next_number=1).
3. `issueAndIncrement` → 1 발급. → `SO-20260524-001` 반환.
4. 그런데 DB 에 이미 시드된 `SO-20260524-001` 이 있어 UNIQUE 위반!

→ 시퀀스 미리 채워주기 = **시드와 발급기의 정합성 유지**.

---

## 🔥 마이그레이션 순서 — FK 의존 따라가기

```
V1   (헬스체크 더미)
V2   code_sequence
V3   customer    ─┐
V4   item        ─┤
V5   vendor      ─┤   FK 없음 (마스터끼리는 독립)
V6   department  ─┤
V7   employee    ─┘   FK → department
V8   시드 (마스터)
V9   code_sequence 일반화
V10  quotation                  FK → customer, item
V11  sales_order                FK → customer, employee, quotation, item
V12  delivery                   FK → sales_order, sales_order_line
V13  invoice                    FK → sales_order, sales_order_line
V14  시드 (DRAFT 견적/수주)
```

V10 → V11 → V12 → V13 의 순서가 강제된다:
- V11 (sales_order) 이 V10 (quotation) 의 FK 참조.
- V12 (delivery) 가 V11 (sales_order) 의 FK 참조.
- V13 (invoice) 도 V11 참조.

→ 마이그레이션 번호 = **FK 의존 그래프의 위상 정렬**.

---

## 자기 점검

- [ ] 트랜잭션 테이블에 `deleted_at` 컬럼이 없는 이유는?
- [ ] `sales_order` 가 `status` 인덱스를 만들었는데 `quotation` 은 안 만든 이유는?
- [ ] `delivery_line` 에 `item_id` 가 없는 이유는?
- [ ] V14 가 "트랜잭션 시드 안 한다" 원칙을 어긴 이유는?
- [ ] 시퀀스 초기화 INSERT 가 V14 마지막에 들어가야 하는 이유는?

---

이전 편 → [05-신용한도-검증.md](./05-신용한도-검증.md)
다음 편 → [07-테스트-시나리오.md](./07-테스트-시나리오.md)
