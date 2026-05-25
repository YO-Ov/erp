-- Phase 2: 출하(Delivery) — 한 수주의 라인을 부분적으로 출하.
--
-- 비즈니스 규칙:
--  - number UNIQUE (DLV-YYYYMMDD-NNN).
--  - 라인은 sales_order_line 을 직접 참조 (item 은 SO 라인에서 따라간다).
--  - 확정(SHIPPED) 시점에 SO 라인의 shipped_qty 가 증가하고, SO 헤더 상태가 SHIPPING/SHIPPED 로 전이.

CREATE TABLE delivery (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    number          VARCHAR(30)     NOT NULL                COMMENT '예: DLV-20260524-001',
    sales_order_id  BIGINT          NOT NULL,
    status          VARCHAR(16)     NOT NULL                COMMENT 'DRAFT/SHIPPED/CANCELLED',
    shipped_date    DATE            NOT NULL,
    created_at      DATETIME        NOT NULL,
    created_by      VARCHAR(64)     NOT NULL,
    updated_at      DATETIME        NOT NULL,
    updated_by      VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_delivery_number (number),
    KEY idx_delivery_sales_order (sales_order_id),
    KEY idx_delivery_shipped_date (shipped_date),
    CONSTRAINT fk_delivery_sales_order FOREIGN KEY (sales_order_id) REFERENCES sales_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='출하 헤더';

CREATE TABLE delivery_line (
    id                    BIGINT          NOT NULL AUTO_INCREMENT,
    delivery_id           BIGINT          NOT NULL,
    sales_order_line_id   BIGINT          NOT NULL,
    line_no               INT             NOT NULL,
    quantity              DECIMAL(15, 4)  NOT NULL,
    created_at            DATETIME        NOT NULL,
    created_by            VARCHAR(64)     NOT NULL,
    updated_at            DATETIME        NOT NULL,
    updated_by            VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_delivery_line_delivery (delivery_id),
    KEY idx_delivery_line_sol (sales_order_line_id),
    CONSTRAINT fk_delivery_line_delivery FOREIGN KEY (delivery_id) REFERENCES delivery(id),
    CONSTRAINT fk_delivery_line_sol FOREIGN KEY (sales_order_line_id) REFERENCES sales_order_line(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='출하 라인 (SO 라인 단위로 부분 출하)';
