-- Phase 2: 견적(Quotation) — 헤더 + 라인.
--
-- 비즈니스 규칙:
--  - number 는 UNIQUE (Q-YYYYMMDD-NNN).
--  - status 변경은 도메인 메서드 (DRAFT → SENT → ACCEPTED/EXPIRED, CANCELLED).
--  - total_amount 는 도메인 메서드에서 재계산 (denormalization).
--  - 트랜잭션은 Soft Delete 미적용 — 잘못 입력은 CANCELLED 상태로 표현.

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
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    quotation_id   BIGINT         NOT NULL,
    line_no        INT            NOT NULL,
    item_id        BIGINT         NOT NULL,
    quantity       DECIMAL(15, 4) NOT NULL,
    unit_price     DECIMAL(15, 2) NOT NULL,
    line_total     DECIMAL(15, 2) NOT NULL,
    created_at     DATETIME       NOT NULL,
    created_by     VARCHAR(64)    NOT NULL,
    updated_at     DATETIME       NOT NULL,
    updated_by     VARCHAR(64)    NOT NULL,
    PRIMARY KEY (id),
    KEY idx_quotation_line_quotation (quotation_id),
    KEY idx_quotation_line_item (item_id),
    CONSTRAINT fk_quotation_line_quotation FOREIGN KEY (quotation_id) REFERENCES quotation(id),
    CONSTRAINT fk_quotation_line_item FOREIGN KEY (item_id) REFERENCES item(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='견적 라인';
