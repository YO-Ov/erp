# 8/9. Flyway 마이그레이션 V2 ~ V8

> Phase 1 의 모든 테이블 DDL + 시드 데이터. 자바 엔티티가 어떤 DDL로 표현되는지 보면서 양쪽을 비교한다.

대상 파일:

```
hwlee-erp/src/main/resources/db/migration/
├─ V2__create_code_sequence.sql
├─ V3__create_customer.sql
├─ V4__create_item.sql
├─ V5__create_vendor.sql
├─ V6__create_department.sql
├─ V7__create_employee.sql       (FK: department_id → department)
└─ V8__seed_master_data.sql
```

(Phase 0 의 V1 은 헬스체크용 더미 테이블)

---

## 🔥 한 파일 = 한 테이블 원칙

Phase 0 워크스루 [04편](../../01-phase-0-환경-구축/3-코드-워크스루/04-Flyway-마이그레이션.md)에서 Flyway의 동작 원리(`flyway_schema_history`, V*.sql 순서, 절대 수정 금지 등)는 짚었으니, 여기서는 **Phase 1의 7개 파일이 왜 이렇게 분할되었는가**에 집중한다.

### 왜 V2~V7 을 한 파일로 합치지 않는가

```
V2__create_master_tables.sql   ← 안 그렇게 했다
```

이 식으로 하나로 합쳐도 동작은 한다. 그런데 다음 두 가지 이유로 **한 파일 = 한 테이블** 로 쪼갰다:

1. **변경 이력 추적**: "Customer 테이블이 언제 만들어졌나?" → `git log V3__create_customer.sql` 한 줄. 합쳐 두면 grep 으로 큰 파일을 뒤져야 함.
2. **롤백 단위**: 운영 단계에서 "Item 테이블만 다른 환경으로 옮기자" 같은 요구가 들어왔을 때 단위가 명확해야 한다.

→ 마이그레이션은 **변경 단위가 곧 파일 단위**가 되는 습관이 좋다.

### 순서가 강제되는 이유 — FK

V6 (Department) 이 V7 (Employee) 보다 먼저 와야 한다. Employee 가 Department 를 FK로 참조하므로:

```sql
-- V7
CONSTRAINT fk_employee_department FOREIGN KEY (department_id) REFERENCES department(id)
```

Department 테이블이 없으면 이 제약을 만들 수 없다. → 마이그레이션 순서는 **FK 의존 방향을 따라간다**.

V2~V5 (code_sequence, customer, item, vendor) 는 서로 의존이 없어서 순서를 바꿔도 동작 자체는 가능하지만, 번호로 묶어 둠으로써 정신적 부담을 줄인다.

---

## 🔥 V2 — `code_sequence`

```sql
CREATE TABLE code_sequence (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    prefix      VARCHAR(16)  NOT NULL COMMENT '코드 접두어 (CUST/ITEM/VEND/EMP 등)',
    year        INT          NOT NULL COMMENT '발급 연도 (4자리)',
    next_number INT          NOT NULL COMMENT '다음에 발급할 일련번호 (1부터 시작)',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_sequence_prefix_year (prefix, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='마스터 비즈니스 코드 발급 시퀀스';
```

### 🔥 `UNIQUE KEY (prefix, year)` 가 동시성의 핵심

02편의 `CodeGenerator.createOrLockExisting(...)` 의 catch 분기가 잡는 게 바로 이 UNIQUE 위반이다.

```
T1: INSERT (prefix='CUST', year=2026, next_number=1) → 성공
T2: INSERT (prefix='CUST', year=2026, next_number=1) → DataIntegrityViolation
     → catch → findForUpdate 로 재진입 → T1 행을 락
```

**자바 코드와 DDL이 짝을 이룬다**. 둘 중 하나만 있으면 안전망에 구멍이 생긴다.

### `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`

MySQL 의 특수 기능. `updated_at` 컬럼을:
- INSERT 시: 현재 시각 자동 입력 (`DEFAULT CURRENT_TIMESTAMP`)
- UPDATE 시: 현재 시각으로 자동 갱신 (`ON UPDATE CURRENT_TIMESTAMP`)

`CodeSequence` 엔티티의 `@LastModifiedDate` 가 어차피 같은 일을 하지만, DDL 디폴트도 같이 두면 **JPA가 우회되어도 (예: 직접 SQL 입력)** 안전망이 된다.

### `BaseEntity` 의 4개 컬럼이 없는 이유

01편/02편에서 짚었듯 이 테이블은 `BaseEntity` 를 상속하지 않는 운영용 카운터. 그래서 DDL에도 `created_at/by`, `updated_by` 컬럼이 없다.

---

## 🔥 V3 — `customer`

```sql
CREATE TABLE customer (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    code          VARCHAR(30)     NOT NULL,
    name          VARCHAR(200)    NOT NULL,
    business_no   VARCHAR(20)     NOT NULL,
    address       VARCHAR(500),
    credit_limit  DECIMAL(15, 2)  NOT NULL DEFAULT 0,
    payment_terms VARCHAR(30)     NOT NULL,
    status        VARCHAR(16)     NOT NULL,
    created_at    DATETIME        NOT NULL,
    created_by    VARCHAR(64)     NOT NULL,
    updated_at    DATETIME        NOT NULL,
    updated_by    VARCHAR(64)     NOT NULL,
    deleted_at    DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_code (code),
    UNIQUE KEY uk_customer_business_no (business_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 자바 엔티티와 1:1 매핑

01편/04편에서 본 `Customer` + `BaseEntityWithCode` + `BaseEntity` 의 모든 필드가 여기 컬럼으로 펼쳐져 있다:

| 컬럼 | 출처 |
|---|---|
| `id` | `BaseEntity` |
| `code`, `status`, `deleted_at` | `BaseEntityWithCode` |
| `created_at`, `created_by`, `updated_at`, `updated_by` | `BaseEntity` |
| `name`, `business_no`, `address`, `credit_limit`, `payment_terms` | `Customer` |

**상속 → 컬럼** 의 매핑을 눈으로 확인할 수 있는 자리.

### 🔥 인덱스 결정의 미묘한 부분

주석에 명시되어 있다:

```sql
-- 인덱스 결정:
--  - code: UNIQUE (단건 조회 + 외부 식별자)
--  - business_no: UNIQUE (중복 방지 + 사업자번호 검색)
--  - name: LIKE '%키워드%' 검색 패턴이라 인덱스 효과 낮음 → 인덱스 생략
--  - status: 카디널리티 낮음 → 인덱스 생략 (필요 시 Phase 후반에 보강)
```

`name` 에 인덱스를 안 거는 이유:

- `LIKE '강남%'` 같은 **앞부분 일치**는 인덱스가 작동.
- `LIKE '%강남%'` 같은 **중간 일치**는 인덱스 무용지물 — 풀스캔.

Customer 의 `nameContains` (`%키워드%`) 가 후자라서 인덱스를 걸어도 효과 없음. 인덱스가 많으면 INSERT/UPDATE 가 느려지므로 **효과 없는 인덱스는 안 거는 게 맞다**.

`status` 의 카디널리티 — 3개 값(ACTIVE/INACTIVE/BLOCKED) 으로 분포되니 인덱스로 좁히는 효과 미미. 운영 데이터가 쌓여서 90%가 ACTIVE 라면 인덱스가 더 손해다. **필요할 때 보강** 이라는 원칙.

### `DEFAULT 0` 이 한 컬럼만 — `credit_limit`

```sql
credit_limit  DECIMAL(15, 2)  NOT NULL DEFAULT 0,
```

자바 쪽에서 `BigDecimal creditLimit` 에 디폴트가 없는데 DDL 디폴트는 0이다. 왜?

- JPA를 통해서 INSERT 하면 자바가 `creditLimit` 을 항상 채워 넣으므로 디폴트가 작동할 일이 없다.
- **시드 / 마이그레이션 / 직접 SQL** 같은 우회 경로에서는 디폴트가 안전망.

→ 자바 검증 + DDL 디폴트 **이중 안전망** 패턴.

### `deleted_at DATETIME` — nullable

```sql
deleted_at    DATETIME,
```

NOT NULL 안 붙음. NULL = "삭제 안 됨" 이 정상 상태이기 때문. 다른 마스터 테이블도 동일.

---

## 🔥 V4 — `item`

```sql
CREATE TABLE item (
    ...
    standard_cost  DECIMAL(15, 2)  NOT NULL,
    standard_price DECIMAL(15, 2)  NOT NULL,
    ...
    PRIMARY KEY (id),
    UNIQUE KEY uk_item_code (code),
    KEY idx_item_category (category)
);
```

### Customer 와의 차이 — `idx_item_category`

Item 에는 `category` 인덱스가 추가로 있다. 검색 시나리오를 보면:

```java
// ItemController
where(nameContains(name)).and(categoryEquals(category)).and(statusEquals(status))
```

`category` 는 **정확 일치 (`=`)** 로 필터링되고, NOTEBOOK / MONITOR 의 2가지 값만 있어 카디널리티가 낮긴 하지만 — 화면에서 "노트북만 보기" 가 자주 일어나는 시나리오라 미리 인덱스를 깔았다.

Customer 의 `status` 는 같은 카디널리티지만 검색 빈도가 낮을 거란 판단 → 안 검 거는 결정. **인덱스는 "걸어두면 좋은" 게 아니라 "비용 대비 효과" 로 판단**.

### `UNIQUE KEY uk_item_code` 뿐 — `business_no` 가 없음

Item은 외부 식별자 없이 `code` 만 UNIQUE. 05편에서 짚었듯 자기 인덱스 외에 별도 비즈니스 식별자가 없는 도메인.

---

## 🔥 V5 — `vendor`

```sql
CREATE TABLE vendor (
    ...
    payment_terms VARCHAR(30)     NOT NULL,
    -- credit_limit 컬럼 없음
    ...
    UNIQUE KEY uk_vendor_code (code),
    UNIQUE KEY uk_vendor_business_no (business_no)
);
```

Customer 와 거의 동일 구조, **`credit_limit` 만 없음**. 05편에서 짚은 비대칭이 DDL에도 그대로 나타남.

---

## 🔥 V6 — `department` (자기참조 FK)

```sql
CREATE TABLE department (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(30)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    parent_id  BIGINT                               COMMENT '상위 부서 id (NULL=루트)',
    status     VARCHAR(16)  NOT NULL,
    ...
    PRIMARY KEY (id),
    UNIQUE KEY uk_department_code (code),
    KEY idx_department_parent_id (parent_id),
    CONSTRAINT fk_department_parent FOREIGN KEY (parent_id) REFERENCES department(id)
);
```

### 🔥 같은 테이블을 FK 로 참조

`REFERENCES department(id)` — DB가 자기참조를 지원한다. 06편의 자바 엔티티 `private Department parent;` 의 DB 표현.

`parent_id` 가 NULL 가능 → 루트 부서를 허용.

### `idx_department_parent_id` — 별도 인덱스 명시

```sql
KEY idx_department_parent_id (parent_id),
```

MySQL은 FK 컬럼에 자동으로 인덱스를 만들어주긴 하는데, **명시적으로 적어두는 게 안전하다** — DB 종류에 따라 자동 인덱스 동작이 다르고, 이름도 결정적이지 않을 수 있다. 직접 박아두면 운영/마이그레이션 시 혼란이 적다.

### 무엇이 의도적으로 빠졌나 — `ON DELETE` 정책

```sql
FOREIGN KEY (parent_id) REFERENCES department(id)
```

`ON DELETE CASCADE` 도, `ON DELETE SET NULL` 도 없다. → **기본값은 `RESTRICT`** — 부모 부서를 삭제하려고 하면 자식이 있으면 거부.

여기에 어플리케이션 레벨 Soft Delete 가 또 끼어든다 — `repository.delete(parent)` 는 `UPDATE department SET deleted_at = NOW()` 로 변환. 즉 진짜 DELETE가 안 일어나서 FK가 보호할 수 없다. **Soft Delete + 자기참조 FK** 조합은 잠재적 미스매치 — 부모를 soft delete 해도 자식은 그대로 살아 있는 상태가 가능.

→ 실무에서는 "휴면 부서로 옮길 때 자식 부서도 같이 휴면 처리" 같은 비즈니스 로직이 추가된다. Phase 1 에서는 학습 단계라 다루지 않음.

---

## 🔥 V7 — `employee`

```sql
CREATE TABLE employee (
    ...
    email         VARCHAR(200) NOT NULL                COMMENT 'Phase 6 부터 로그인 ID 로 사용',
    department_id BIGINT       NOT NULL,
    hire_date     DATE         NOT NULL,
    ...
    UNIQUE KEY uk_employee_code (code),
    UNIQUE KEY uk_employee_email (email),
    KEY idx_employee_department_id (department_id),
    CONSTRAINT fk_employee_department FOREIGN KEY (department_id) REFERENCES department(id)
);
```

### `department_id NOT NULL`

엔티티의 `@JoinColumn(nullable = false)` + `@ManyToOne(optional = false)` 의 DB 표현. 07편에서 짚은 "직원은 반드시 부서에 속한다" 가 컬럼 레벨로 강제된다.

### `hire_date DATE` (DATETIME 아님)

07편에서 짚었던 "날짜만 의미 있고 시각은 의미 없다" 가 컬럼 타입에 반영. MySQL의 `DATE` 타입은 시각 정보 없이 날짜만 저장.

### 이메일에 추가 검증 인덱스가 없는 이유

`uk_employee_email` 이 UNIQUE 인덱스로 같이 작동한다. UNIQUE는 자동으로 인덱스 — 별도 `INDEX (email)` 을 만들면 중복. 그래서 UNIQUE 만으로 충분.

---

## 🔥 V8 — 시드 데이터의 미묘한 기교

```sql
-- 1) 루트 부서 (회사)
INSERT INTO department (code, name, parent_id, status, ...)
VALUES ('DEPT-HQ', '현우전자', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL);

SET @hq_id = LAST_INSERT_ID();

-- 2) 하위 부서
INSERT INTO department ... VALUES
    ('DEPT-SALES',      '영업팀', @hq_id, ...),
    ('DEPT-PURCHASE',   '구매팀', @hq_id, ...),
    ...
```

### `LAST_INSERT_ID()` + 변수 — id가 정해지지 않은 INSERT 의 자식을 또 INSERT

자동 증가 `id` 라서 `DEPT-HQ` 가 받을 id 를 미리 모른다. MySQL의 `LAST_INSERT_ID()` 가 직전 INSERT 의 AUTO_INCREMENT 값을 돌려준다. 그걸 변수에 저장해서 자식 부서들의 `parent_id` 에 채움.

다른 DB (PostgreSQL) 라면 `RETURNING id` 를 사용하는 등 표현이 다르다. **시드 SQL 은 DB-specific** 한 부분이 있다.

### `SELECT` 로 INSERT — Employee 의 부서 참조

```sql
INSERT INTO employee (code, name, email, department_id, hire_date, ...)
SELECT CONCAT('EMP-', YEAR(NOW()), '-0001'), '김영업', 'kim@hwlee-erp.example',
       d.id, '2025-01-15', 'ACTIVE', ...
  FROM department d WHERE d.code = 'DEPT-SALES';
```

영업팀의 `id` 를 `code` 로 조회해서 그대로 INSERT 의 값으로 사용 — **`INSERT ... SELECT`** 패턴. 변수에 저장하지 않고도 부서 id 를 가져올 수 있다.

→ Department 코드는 의미 있는 값이라 "이 직원은 영업팀에 소속" 같은 의도를 SQL 안에서 그대로 읽을 수 있다.

### code_sequence 초기화 — 시드와 자동 발급의 동기화

```sql
-- 3) 코드 시퀀스 초기화 — 시드된 직원 다음 번호부터 발급되도록.
INSERT INTO code_sequence (prefix, year, next_number, updated_at) VALUES
    ('CUST', YEAR(NOW()), 3, NOW()),    -- 시드 고객 2건이 0001, 0002 → 다음은 0003
    ('ITEM', YEAR(NOW()), 3, NOW()),
    ('VEND', YEAR(NOW()), 2, NOW()),
    ('EMP',  YEAR(NOW()), 4, NOW());
```

이게 미묘한데 매우 중요한 부분이다. 시드 데이터로 Customer 2건을 미리 박은 뒤에 `code_sequence` 의 `next_number` 를 3으로 맞춰둔다. 안 그러면:

```
시드 후 첫 신규 등록:
   codeGenerator.nextCode("CUST")
   → code_sequence 가 비어 있으니 새로 만듦 → next_number=1
   → CUST-2026-0001 발급 시도
   → customer 테이블에 CUST-2026-0001 이미 존재 (시드)
   → UNIQUE 위반 → 실패
```

→ **시드 데이터와 자동 발급기는 같은 카운터 공간을 공유**하므로, 시드가 카운터를 점유한 만큼 미리 진도를 빼둬야 한다.

### `YEAR(NOW())` — 현재 연도 동적

```sql
INSERT INTO ... VALUES ('CUST', YEAR(NOW()), 3, NOW())
```

하드코딩으로 `'CUST', 2026` 안 박고 동적으로 가져옴. 이렇게 하면 2027년이 와서 시드가 다시 적용될 때도(개발 환경 재초기화 등) 자연스럽게 그 해 시퀀스를 만든다.

다만 한 가지 주의: **시드 INSERT 들이 모두 `NOW()` 의 같은 해** 일 가정. 자정 직전에 시드가 절반만 적용되고 자정 직후에 나머지가 적용되면 어긋난다. → 일반적으로 시드는 한 번에 끝나는 마이그레이션이라 문제 없음.

---

## 🔥 마이그레이션 운영 원칙 — 한 번 더

Phase 0 워크스루에서도 강조한 부분, Phase 1 의 V8 까지 적용된 지금 다시 짚기:

| 절대 안 되는 것 | 왜 |
|---|---|
| V3 파일 내용 수정 | 이미 운영에 적용된 DDL을 바꾸는 것 = 환경 간 스키마 분기 |
| V3, V4 순서 바꾸기 | 같은 이유 |
| 새 V를 끼워 넣기 (예: V3.5) | flyway_schema_history 와 충돌 가능 |
| V8 시드를 운영 DB에 또 적용 | UNIQUE 위반으로 깨짐 (보호되긴 하지만 운영 인시던트) |

대신 항상 **새 V** 를 추가한다:
- "Customer에 email 컬럼 추가" → `V9__add_email_to_customer.sql`
- "잘못 시드된 고객 삭제" → `V10__remove_wrong_customer.sql`

---

## 자기 점검

- [ ] V6 → V7 순서가 강제되는 도메인 이유?
- [ ] `name` 에 인덱스를 안 거는 이유 (Customer DDL 주석 참조)?
- [ ] `code_sequence` 시드의 `next_number=3` 이 의미하는 것은?
- [ ] V3 파일을 수정해서 컬럼을 추가하면 어떤 문제가 발생하는가?
- [ ] Soft Delete + FK 의 잠재적 미스매치를 한 시나리오로 설명할 수 있는가?

---

이전 편 → [07-Employee-Department-FK.md](./07-Employee-Department-FK.md)
다음 편 → [09-테스트-동시성과-CRUD.md](./09-테스트-동시성과-CRUD.md)
