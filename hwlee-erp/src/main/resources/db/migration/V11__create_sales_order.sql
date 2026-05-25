-- Phase 2: 수주(SalesOrder) — OTC 흐름의 중심축.
--
-- 비즈니스 규칙:
--  - number UNIQUE (SO-YYYYMMDD-NNN).
--  - 신용한도/고객 ACTIVE 검증은 confirm() 시점에 서비스가 처리.
--  - 라인의 shipped_qty, invoiced_qty 가 부분 출하/청구의 단일 진실.
--    shipped_qty <= order_qty, invoiced_qty <= shipped_qty (도메인 메서드가 강제).

CREATE TABLE sales_order (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    number          VARCHAR(30)     NOT NULL                COMMENT '예: SO-20260524-001',
    customer_id     BIGINT          NOT NULL,
    salesperson_id  BIGINT                                  COMMENT 'Employee (담당자, 선택)',
    quotation_id    BIGINT                                  COMMENT '견적에서 유래한 경우 (선택)',
    status          VARCHAR(16)     NOT NULL                COMMENT 'DRAFT/CONFIRMED/SHIPPING/SHIPPED/INVOICING/INVOICED/CLOSED/CANCELLED',
    order_date      DATE            NOT NULL,
    confirmed_at    DATETIME                                COMMENT '확정 시각 (감사용)',
    total_amount    DECIMAL(15, 2)  NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL,
    created_by      VARCHAR(64)     NOT NULL,
    updated_at      DATETIME        NOT NULL,
    updated_by      VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sales_order_number (number),
    KEY idx_sales_order_customer (customer_id),
    KEY idx_sales_order_status (status),
    KEY idx_sales_order_order_date (order_date),
    CONSTRAINT fk_sales_order_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_sales_order_salesperson FOREIGN KEY (salesperson_id) REFERENCES employee(id),
    CONSTRAINT fk_sales_order_quotation FOREIGN KEY (quotation_id) REFERENCES quotation(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='수주 헤더';

CREATE TABLE sales_order_line (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    sales_order_id  BIGINT          NOT NULL,
    line_no         INT             NOT NULL,
    item_id         BIGINT          NOT NULL,
    order_qty       DECIMAL(15, 4)  NOT NULL                COMMENT '주문량 (확정 후 불변)',
    shipped_qty     DECIMAL(15, 4)  NOT NULL DEFAULT 0      COMMENT '출하 누적',
    invoiced_qty    DECIMAL(15, 4)  NOT NULL DEFAULT 0      COMMENT '청구 누적',
    unit_price      DECIMAL(15, 2)  NOT NULL,
    line_total      DECIMAL(15, 2)  NOT NULL                COMMENT 'order_qty * unit_price (공급가)',
    created_at      DATETIME        NOT NULL,
    created_by      VARCHAR(64)     NOT NULL,
    updated_at      DATETIME        NOT NULL,
    updated_by      VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_sales_order_line_order (sales_order_id),
    KEY idx_sales_order_line_item (item_id),
    CONSTRAINT fk_sales_order_line_order FOREIGN KEY (sales_order_id) REFERENCES sales_order(id),
    CONSTRAINT fk_sales_order_line_item FOREIGN KEY (item_id) REFERENCES item(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='수주 라인 (부분 출하/청구의 단일 진실)';
