-- Phase 3: 출고 (GoodsIssue) — 창고에서 실제로 물건이 빠져나가는 사건.
--
-- 비즈니스 규칙:
--  - number UNIQUE (GI-YYYYMMDD-NNN).
--  - post() 시점에 라인별로 stockRepo.findForUpdate (PESSIMISTIC_WRITE) → 가용 검증 → Stock.issue() → StockMovement(-) 적재.
--  - 가용 재고 < 요청 수량이면 409 INSUFFICIENT_STOCK (도메인 예외).
--  - Phase 3 의 출고는 reason 만으로 분류 (SHIPMENT/ADJUSTMENT/SCRAP) — Customer/SO 정보는 Phase 4 의 SD↔MM 연계에서 추가.

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
