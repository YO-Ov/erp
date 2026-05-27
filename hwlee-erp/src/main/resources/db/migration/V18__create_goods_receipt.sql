-- Phase 3: 입고 (GoodsReceipt) — 매입처에서 받은 사건을 헤더-라인으로 기록.
--
-- 비즈니스 규칙:
--  - number UNIQUE (GR-YYYYMMDD-NNN).
--  - post() 시점에 라인별로 Stock.receive() (가중평균 단가 갱신) + StockMovement(+) 적재.
--  - 같은 (item, warehouse) 동시 입고는 Stock.@Version (낙관적 락) 으로 보호.

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
