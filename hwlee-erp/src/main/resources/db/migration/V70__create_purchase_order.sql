-- 구매발주(Purchase Order) — 거래처 매입 발주 문서 + 전자결재 전결 규정.
-- 흐름: DRAFT →(전자결재 승인)→ CONFIRMED →(입고 완료)→ CLOSED. 결재 없이는 확정 불가.

CREATE TABLE purchase_order (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    number        VARCHAR(30)   NOT NULL COMMENT '발주번호 PORD-YYYYMMDD-NNN',
    vendor_id     BIGINT        NOT NULL,
    warehouse_id  BIGINT        NOT NULL COMMENT '입고 예정 창고',
    status        VARCHAR(16)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/CLOSED/CANCELLED',
    order_date    DATE          NOT NULL,
    expected_date DATE          NULL COMMENT '입고 예정일(희망 납기)',
    remark        VARCHAR(500)  NULL,
    created_at    DATETIME(6)   NOT NULL,
    created_by    VARCHAR(100)  NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    updated_by    VARCHAR(100)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_purchase_order_number (number),
    KEY idx_purchase_order_vendor (vendor_id),
    KEY idx_purchase_order_status (status, order_date),
    CONSTRAINT fk_po_vendor    FOREIGN KEY (vendor_id)    REFERENCES vendor (id),
    CONSTRAINT fk_po_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='구매발주 헤더';

CREATE TABLE purchase_order_line (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    purchase_order_id BIGINT        NOT NULL,
    line_no           INT           NOT NULL,
    item_id           BIGINT        NOT NULL,
    quantity          DECIMAL(18,4) NOT NULL,
    unit_price        DECIMAL(15,2) NOT NULL COMMENT '매입 단가',
    line_total        DECIMAL(15,2) NOT NULL,
    created_at        DATETIME(6)   NOT NULL,
    created_by        VARCHAR(100)  NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    updated_by        VARCHAR(100)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_po_line_order (purchase_order_id),
    CONSTRAINT fk_po_line_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_order (id),
    CONSTRAINT fk_po_line_item  FOREIGN KEY (item_id)           REFERENCES item (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='구매발주 라인';

-- 전결 규정(구매발주) — 지출 문서. 1천만 미만 팀장 / 1천만~5천만 본부장 / 5천만 이상 대표+재무합의.
INSERT INTO approval_rule (doc_type, min_amount, max_amount, approval_level, require_finance_agreement, created_at, created_by, updated_at, updated_by) VALUES
    ('PURCHASE_ORDER',        0.00, 10000000.00, 'TEAM',     0, NOW(), 'system', NOW(), 'system'),
    ('PURCHASE_ORDER', 10000000.00, 50000000.00, 'DIVISION', 0, NOW(), 'system', NOW(), 'system'),
    ('PURCHASE_ORDER', 50000000.00, NULL,        'COMPANY',  1, NOW(), 'system', NOW(), 'system');
