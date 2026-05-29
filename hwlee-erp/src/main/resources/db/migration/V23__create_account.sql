-- Phase 5: 계정과목(Account) — 회계 원장의 첫 번째 마스터.
--
-- 비즈니스 규칙:
--  - 자기참조 트리 (자산 1000 → 매출채권 1200). Department 패턴 재사용.
--  - type 컬럼이 정상 잔액 방향의 진실(ASSET/EXPENSE=차변, LIABILITY/EQUITY/REVENUE=대변).
--    별도의 normal_side 컬럼은 두지 않는다 — 유형이 곧 방향.
--  - postable=N 인 헤더 계정(자산/부채/수익/비용)에는 직접 전표를 달 수 없다.
--    말단 계정(매출채권/매출/매출원가/현금 등)만 라인의 account_id 로 쓰일 수 있다.
--  - 자동 코드 생성은 쓰지 않는다 — 회계 계정 코드는 의미 있는 명시적 값(1100, 4100 등).

CREATE TABLE account (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(30)  NOT NULL                COMMENT '회계 코드 (1100/4100 등)',
    name       VARCHAR(100) NOT NULL                COMMENT '계정명 (현금/매출 등)',
    type       VARCHAR(16)  NOT NULL                COMMENT 'ASSET/LIABILITY/EQUITY/REVENUE/EXPENSE',
    parent_id  BIGINT                               COMMENT '상위 계정 (NULL=루트)',
    postable   TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '1=라인 가능 / 0=헤더 계정',
    status     VARCHAR(16)  NOT NULL,
    created_at DATETIME     NOT NULL,
    created_by VARCHAR(64)  NOT NULL,
    updated_at DATETIME     NOT NULL,
    updated_by VARCHAR(64)  NOT NULL,
    deleted_at DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_code (code),
    KEY idx_account_parent_id (parent_id),
    KEY idx_account_type (type),
    CONSTRAINT fk_account_parent FOREIGN KEY (parent_id) REFERENCES account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='계정과목 마스터 (자기참조 트리)';

-- 시드 — 학습에 필요한 최소 12개 계정. 부모 → 자식 순서 INSERT (FK 제약).
-- 주의: 부가세대급금(2300)은 매입 시 미리 낸 부가세이므로 유형이 ASSET (코드 번호와 유형은 1:1 아님).
INSERT INTO account (code, name, type, parent_id, postable, status,
                     created_at, created_by, updated_at, updated_by)
VALUES
    ('1000', '자산',          'ASSET',     NULL, 0, 'ACTIVE', NOW(), 'system', NOW(), 'system'),
    ('2000', '부채',          'LIABILITY', NULL, 0, 'ACTIVE', NOW(), 'system', NOW(), 'system'),
    ('4000', '수익',          'REVENUE',   NULL, 0, 'ACTIVE', NOW(), 'system', NOW(), 'system'),
    ('5000', '비용',          'EXPENSE',   NULL, 0, 'ACTIVE', NOW(), 'system', NOW(), 'system');

-- 말단 계정 (postable=1) — 부모는 코드로 lookup.
INSERT INTO account (code, name, type, parent_id, postable, status,
                     created_at, created_by, updated_at, updated_by)
SELECT '1100', '현금',          'ASSET',     id, 1, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM account WHERE code = '1000';
INSERT INTO account (code, name, type, parent_id, postable, status,
                     created_at, created_by, updated_at, updated_by)
SELECT '1200', '매출채권',      'ASSET',     id, 1, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM account WHERE code = '1000';
INSERT INTO account (code, name, type, parent_id, postable, status,
                     created_at, created_by, updated_at, updated_by)
SELECT '1400', '재고자산',      'ASSET',     id, 1, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM account WHERE code = '1000';
INSERT INTO account (code, name, type, parent_id, postable, status,
                     created_at, created_by, updated_at, updated_by)
SELECT '2300', '부가세대급금',  'ASSET',     id, 1, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM account WHERE code = '1000';

INSERT INTO account (code, name, type, parent_id, postable, status,
                     created_at, created_by, updated_at, updated_by)
SELECT '2100', '매입채무',      'LIABILITY', id, 1, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM account WHERE code = '2000';
INSERT INTO account (code, name, type, parent_id, postable, status,
                     created_at, created_by, updated_at, updated_by)
SELECT '2200', '부가세예수금',  'LIABILITY', id, 1, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM account WHERE code = '2000';

INSERT INTO account (code, name, type, parent_id, postable, status,
                     created_at, created_by, updated_at, updated_by)
SELECT '4100', '매출',          'REVENUE',   id, 1, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM account WHERE code = '4000';

INSERT INTO account (code, name, type, parent_id, postable, status,
                     created_at, created_by, updated_at, updated_by)
SELECT '5100', '매출원가',      'EXPENSE',   id, 1, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM account WHERE code = '5000';
