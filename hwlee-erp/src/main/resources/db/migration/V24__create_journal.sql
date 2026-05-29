-- Phase 5: 회계 전표 — JournalEntry(헤더) + JournalLine(라인).
--
-- 비즈니스 규칙:
--  - number UNIQUE (JE-YYYYMMDD-NNN).
--  - status: DRAFT → POSTED(확정 시) → CANCELLED(역분개 별도 전표 생성).
--  - 차변 합 = 대변 합 — post() 단계에서 도메인 검증. 불일치면 UnbalancedJournalException → 422.
--  - 출처: source_type(INV/GI/GR/PAY/MANUAL) + source_id 약한 참조. FK 안 건다(다형성).
--  - 라인은 debit / credit 두 컬럼. 한 라인은 한 쪽만 > 0 (반대쪽 = 0). CHECK 로 강제.
--  - account_id 는 FK 로 묶고, postable=0 헤더 계정 거부는 도메인 코드(addLine)에서 검증.

CREATE TABLE journal_entry (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    number       VARCHAR(30)     NOT NULL                COMMENT '예: JE-20260524-001',
    entry_date   DATE            NOT NULL                COMMENT '전표일자 (회계 마감 기준일)',
    description  VARCHAR(255)    NOT NULL                COMMENT '적요 (사람이 보는 한 줄 설명)',
    status       VARCHAR(16)     NOT NULL                COMMENT 'DRAFT/POSTED/CANCELLED',
    source_type  VARCHAR(16)     NOT NULL                COMMENT 'INV/GI/GR/PAY/MANUAL — 출처 분류',
    source_id    BIGINT                                  COMMENT '출처 트랜잭션 id (MANUAL 이면 NULL)',
    posted_at    DATETIME                                COMMENT '확정 시각 (감사용)',
    created_at   DATETIME        NOT NULL,
    created_by   VARCHAR(64)     NOT NULL,
    updated_at   DATETIME        NOT NULL,
    updated_by   VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_journal_entry_number (number),
    KEY idx_journal_entry_entry_date (entry_date),
    KEY idx_journal_entry_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회계 전표 헤더';

CREATE TABLE journal_line (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    journal_entry_id  BIGINT          NOT NULL,
    line_no           INT             NOT NULL,
    account_id        BIGINT          NOT NULL,
    debit             DECIMAL(15, 2)  NOT NULL DEFAULT 0    COMMENT '차변 (대변 라인이면 0)',
    credit            DECIMAL(15, 2)  NOT NULL DEFAULT 0    COMMENT '대변 (차변 라인이면 0)',
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
