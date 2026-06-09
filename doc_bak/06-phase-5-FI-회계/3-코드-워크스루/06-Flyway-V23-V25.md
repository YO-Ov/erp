# 6/7. Flyway V23~V25 — 스키마와 시드 한 번에

> Phase 5 의 모든 도메인 객체는 세 개의 마이그레이션 위에 서 있다. 이 글은 그 SQL 을 한 줄씩 따라가며, **왜 그렇게 박았는지** 를 본다.
>
> 짚을 포인트: (1) 트리 FK 의 순서 (2) DB CHECK 로 도메인 불변식을 한 번 더 잠금 (3) Payment 의 다형성 party 정합성 (4) 출처 인덱스.

대상 파일:

```
hwlee-erp/src/main/resources/db/migration/
├─ V23__create_account.sql     계정과목 + 12개 시드
├─ V24__create_journal.sql     journal_entry + journal_line + CHECK
└─ V25__create_payment.sql     payment + CHECK
```

---

## 🔥 V23 — 계정과목 + 시드 한 파일에

```sql
CREATE TABLE account (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(30)  NOT NULL,                                       -- 1100, 4100, ...
    name       VARCHAR(100) NOT NULL,
    type       VARCHAR(16)  NOT NULL,                                       -- ASSET/LIABILITY/...
    parent_id  BIGINT,                                                      -- NULL = 루트
    postable   TINYINT(1)   NOT NULL DEFAULT 1,                             -- 1 = 라인 가능
    status     VARCHAR(16)  NOT NULL,
    created_at DATETIME     NOT NULL,
    created_by VARCHAR(64)  NOT NULL,
    updated_at DATETIME     NOT NULL,
    updated_by VARCHAR(64)  NOT NULL,
    deleted_at DATETIME,                                                    -- 소프트 삭제
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_code (code),
    KEY idx_account_parent_id (parent_id),
    KEY idx_account_type (type),
    CONSTRAINT fk_account_parent FOREIGN KEY (parent_id) REFERENCES account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='계정과목 마스터 (자기참조 트리)';
```

Department 와 거의 똑같은 모양. 다른 컬럼:
- `type` — `AccountType` enum 의 문자열 저장. JPA `@Enumerated(EnumType.STRING)` 와 1:1.
- `postable` — `TINYINT(1)` (MySQL 의 boolean 표현 관습).
- `idx_account_type` — 유형별 조회용 인덱스. "모든 자산 계정 보여줘" 같은 리포트 대비.

### 시드 — 부모 → 자식 두 단계

```sql
-- 1) 헤더 계정 4개 (parent_id NULL)
INSERT INTO account (code, name, type, parent_id, postable, ...)
VALUES
    ('1000', '자산',  'ASSET',     NULL, 0, ...),
    ('2000', '부채',  'LIABILITY', NULL, 0, ...),
    ('4000', '수익',  'REVENUE',   NULL, 0, ...),
    ('5000', '비용',  'EXPENSE',   NULL, 0, ...);

-- 2) 말단 계정 — 부모를 코드로 lookup
INSERT INTO account (code, name, type, parent_id, postable, ...)
SELECT '1100', '현금', 'ASSET', id, 1, ...
FROM account WHERE code = '1000';
```

`fk_account_parent` 가 잡혀 있어서, 헤더가 먼저 들어가야 자식 INSERT 가 통과. 시드는 자식의 `parent_id` 를 `(SELECT id FROM account WHERE code = '...')` 로 lookup — **자동 증가 id 에 의존하지 않는** 안전한 방식.

> 💡 만약 `VALUES (..., 1, ...)` 처럼 부모 id 를 하드코딩하면? `AUTO_INCREMENT` 값이 환경마다 다르므로 깨질 수 있다 (특히 같은 테이블에 다른 마이그레이션이 끼어들 때). **id 가 아닌 code 로 lookup** 이 정답.

### `type` 컬럼이 진실 — 코드 번호와의 비대칭

```sql
('2300', '부가세대급금', 'ASSET', id, 1, ...)   -- 코드는 2xxx 인데 유형은 ASSET
```

매입 시 받은 부가세는 "다음 신고 시 차감받을 자산" — 그래서 ASSET. **코드 번호 ≠ 유형**. 비즈니스 로직(`AccountType.getNormalSide()`)이 type 만 보도록 해 둔 결정이 여기서 빛난다 — 코드를 보고 추측하면 부가세대급금의 정상 잔액 방향을 거꾸로 알게 된다.

---

## 🔥 V24 — 전표 헤더 + 라인 + CHECK 두 겹

```sql
CREATE TABLE journal_entry (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    number       VARCHAR(30)     NOT NULL,                               -- JE-YYYYMMDD-NNN
    entry_date   DATE            NOT NULL,
    description  VARCHAR(255)    NOT NULL,
    status       VARCHAR(16)     NOT NULL,                               -- DRAFT/POSTED/CANCELLED
    source_type  VARCHAR(16)     NOT NULL,                               -- INV/GI/GR/PAY/MANUAL
    source_id    BIGINT,                                                  -- MANUAL 이면 NULL
    posted_at    DATETIME,
    created_at   DATETIME        NOT NULL,
    created_by   VARCHAR(64)     NOT NULL,
    updated_at   DATETIME        NOT NULL,
    updated_by   VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_journal_entry_number (number),
    KEY idx_journal_entry_entry_date (entry_date),
    KEY idx_journal_entry_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회계 전표 헤더';
```

- **`(source_type, source_id)` 복합 인덱스** — `findBySourceTypeAndSourceId` 의 핵심. "이 인보이스로 만든 전표" 한 줄 조회 시 인덱스로 직진.
- **`entry_date` 인덱스** — 월/연 단위 리포트 (Phase 10) 대비.
- **`source_id` FK 없음** — 다형성. invoice/goods_issue/goods_receipt/payment 4개 테이블을 한 FK 로 묶을 수 없음. `StockMovement.refType/refId` 와 같은 패턴.

### 라인 + CHECK ⭐

```sql
CREATE TABLE journal_line (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    journal_entry_id  BIGINT          NOT NULL,
    line_no           INT             NOT NULL,
    account_id        BIGINT          NOT NULL,
    debit             DECIMAL(15, 2)  NOT NULL DEFAULT 0,
    credit            DECIMAL(15, 2)  NOT NULL DEFAULT 0,
    created_at        DATETIME        NOT NULL,
    created_by        VARCHAR(64)     NOT NULL,
    updated_at        DATETIME        NOT NULL,
    updated_by        VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_journal_line_entry (journal_entry_id),
    KEY idx_journal_line_account (account_id),
    CONSTRAINT fk_journal_line_entry   FOREIGN KEY (journal_entry_id) REFERENCES journal_entry(id),
    CONSTRAINT fk_journal_line_account FOREIGN KEY (account_id)       REFERENCES account(id),
    -- 한 라인은 한 쪽만 > 0 (둘 다 0 또는 둘 다 > 0 모두 금지).
    CONSTRAINT chk_journal_line_side CHECK (
        (debit > 0 AND credit = 0) OR (debit = 0 AND credit > 0)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회계 전표 라인 (차/대 두 컬럼)';
```

핵심 한 줄 — `chk_journal_line_side` CHECK 제약. 도메인(`JournalLine` 생성자)이 이미 검증하는데도 DB 에 또 박는 이유:

| 도메인 검증만 | DB CHECK 추가 |
| --- | --- |
| 정상 경로(JPA 통한 INSERT) 는 안전 | 비정상 경로도 안전 |
| 직접 SQL INSERT/UPDATE 시 우회 가능 | DB가 거부 |
| 마이그레이션 SQL 실수도 통과 | 부정 행이 들어가지 않음 |

→ **방어선의 마지막 줄**. Phase 3 의 `chk_goods_issue_line_qty CHECK (quantity > 0)` 과 같은 철학 — "도메인이 지키고, DB 가 한 번 더 지킨다".

> 💡 MySQL 8.0.16+ 가 CHECK 제약을 실제로 강제. 그 이전 버전은 무시했었다. 학습 환경의 MySQL 8 이라 안전.

---

## 🔥 V25 — Payment + 다형성 party 정합성

```sql
CREATE TABLE payment (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    number        VARCHAR(30)     NOT NULL,
    type          VARCHAR(16)     NOT NULL,            -- RECEIPT/DISBURSEMENT
    customer_id   BIGINT,                              -- RECEIPT 일 때 필수
    vendor_id     BIGINT,                              -- DISBURSEMENT 일 때 필수
    amount        DECIMAL(15, 2)  NOT NULL,
    payment_date  DATE            NOT NULL,
    status        VARCHAR(16)     NOT NULL,
    posted_at     DATETIME,
    description   VARCHAR(255),
    created_at    DATETIME        NOT NULL,
    ...
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_number (number),
    KEY idx_payment_customer (customer_id),
    KEY idx_payment_vendor (vendor_id),
    KEY idx_payment_date (payment_date),
    CONSTRAINT fk_payment_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_payment_vendor   FOREIGN KEY (vendor_id)   REFERENCES vendor(id),
    -- type 과 party 의 정합성: RECEIPT 면 customer 만, DISBURSEMENT 면 vendor 만.
    CONSTRAINT chk_payment_party CHECK (
        (type = 'RECEIPT'      AND customer_id IS NOT NULL AND vendor_id   IS NULL) OR
        (type = 'DISBURSEMENT' AND vendor_id   IS NOT NULL AND customer_id IS NULL)
    ),
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='입금/출금 헤더';
```

### Payment 다형성 — 두 가지 옵션을 비교

`Payment` 가 거래 상대를 두 형태(`customer` 또는 `vendor`) 로 가진다는 점은 설계 시 고민 거리:

| 옵션 | 표현 |
| --- | --- |
| **B (현 채택)** | `customer_id` / `vendor_id` 두 컬럼 + CHECK 로 둘 중 하나만 채움 |
| A | 추상화된 `party_type` / `party_id` 다형성 약 참조 |
| C | `Payment` 를 `Receipt` / `Disbursement` 두 테이블로 분리 (Joined inheritance) |

**옵션 B 채택** 이유:
- **타입 안전성** — 진짜 FK 로 두 테이블을 직접 참조. JPA `@ManyToOne Customer` / `@ManyToOne Vendor` 가 그대로 동작.
- **CHECK 제약 한 줄로 정합성** — "RECEIPT 면 customer 필수, vendor 금지" 가 DB 에 박혀 안전.
- **단순함** — 옵션 A는 FK 가 안 잡혀 위험, 옵션 C 는 두 테이블 분리 비용.

> 💡 옵션 B 의 약점 — Payment 종류가 5종 10종으로 늘어나면 컬럼이 폭증한다. Phase 5 단계엔 2종이라 깔끔. 5종 넘어가면 옵션 C 가 더 나을 수도.

### CHECK 가 도메인 팩토리와 짝을 이룸

```java
// Payment.java
public static Payment receipt(String number, Customer customer, BigDecimal amount, ...) {
    if (customer == null) {
        throw new IllegalArgumentException("입금(RECEIPT)에는 customer 가 필수다.");
    }
    Payment p = baseOf(number, amount, paymentDate, description);
    p.type = PaymentType.RECEIPT;
    p.customer = customer;     // ← vendor 는 안 건드림 → null
    return p;
}
```

도메인 팩토리(`Payment.receipt` / `Payment.disbursement`) 가 한 쪽만 채우도록 강제. 정상 경로로는 DB CHECK 가 발동할 일이 없지만, 직접 SQL/잘못된 PATCH 등에 대비.

---

## 🔥 마이그레이션 순서 — V22 → V23 → V24 → V25

V23 → V24 → V25 가 일직선 의존:

- V23 (account) 가 먼저. V24 의 `fk_journal_line_account` 가 V23 의 account 참조.
- V25 (payment) 의 `fk_payment_customer/vendor` 는 V3/V5(customer/vendor) 참조 — 이미 존재.

Flyway 가 파일명 버전 순서로 적용 — V22 까지 적용된 환경에 V23/V24/V25 가 차례로 추가된다. 기존 데이터는 무영향.

> 💡 V26 (선택) — `code_sequence(JE, PAY)` 시드는 안 만들었다. `TransactionNumberGenerator.nextJournalEntryNumber` 가 첫 호출 시 `(prefix='JE', period_key='...')` 행이 없으면 자동으로 만들고 1부터 시작 (Phase 1~3 의 패턴). 그래서 V26 불필요.

---

## 🔥 마이그레이션 회고 — 3 파일 vs 1 파일

Phase 2 의 `V13__create_invoice.sql` 처럼 한 파일에 헤더+라인을 묶을 수도 있었지만, Phase 5 는 도메인이 셋(`account` / `journal` / `payment`) 이라 3 파일로 분리:

| 분리의 이득 | 합치는 이득 |
| --- | --- |
| 한 도메인이 한 파일 — 가독성 ↑ | 마이그레이션 적용 횟수 ↓ |
| 후속 Phase 가 한 도메인만 변경 시 작업 명확 | (학습 단계엔 미미) |
| 시드와 테이블이 한 파일에 모여 시각화 좋음 | — |

Phase 1~4 의 관습대로 도메인당 파일 1개를 유지.

---

## 🔥 ERD — Phase 5 한 장 요약

```
                    ┌─────────────┐
                    │   account    │ (자기참조 트리)
                    └──────┬──────┘
                           │ account_id
                           │
                    ┌──────┴──────┐
                    │ journal_line │ ── chk_journal_line_side
                    └──────┬──────┘
                           │ journal_entry_id
                           │
                    ┌──────┴──────┐
                    │journal_entry │ ── (source_type, source_id) 약 참조
                    └─────────────┘
                           ▲
                           │ 자동 분개 생성
                           │
   ┌──────────┐   ┌──────────────┐   ┌──────────┐   ┌──────────┐
   │ invoice  │   │goods_receipt │   │goods_issue│  │ payment  │
   │ (INV)    │   │ (GR)         │   │ (GI)     │   │ (PAY)    │
   └──────────┘   └──────────────┘   └──────────┘   └──────────┘
                                                          │
                                                          │ FK
                                                    ┌─────┴─────┐
                                                    │ customer  │
                                                    │ vendor    │
                                                    └───────────┘
```

→ 회계 라인이 account 로 모이고, 회계 헤더가 다른 도메인(INV/GR/GI/PAY) 을 약 참조. 단방향 의존 — 다른 도메인은 회계를 모름.

---

## 정리

| 학습 포인트 | SQL 위치 |
| --- | --- |
| 자기참조 트리 + 부모→자식 시드 순서 | V23 INSERT 두 단계 |
| 부모 lookup 은 SELECT id FROM ... WHERE code | V23 |
| `type` 이 진실, 코드 번호와 비대칭 (부가세대급금) | V23 시드 |
| `(source_type, source_id)` 복합 인덱스 | V24 |
| `chk_journal_line_side` — DB 가 차/대 한쪽만 양수를 강제 ⭐ | V24 |
| 도메인 + DB 의 2겹 방어 | `JournalLine` 생성자 + V24 CHECK |
| Payment 다형성 — 두 컬럼 + CHECK 정합성 | V25 |
| DB CHECK 와 도메인 팩토리의 짝 | `Payment.receipt/disbursement` + V25 |
| `code_sequence` 의 JE/PAY 시드 불필요 (자동 채번) | V26 안 만듦 |

마지막 글(`07-회계-시나리오-테스트.md`) 에서는 **테스트가 어떻게 이 모든 흐름을 검증하는지** 를 본다.
