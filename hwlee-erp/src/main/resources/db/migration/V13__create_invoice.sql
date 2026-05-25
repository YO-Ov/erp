-- Phase 2: 인보이스(Invoice) — 부가세 10% 단일 정책.
--
-- 비즈니스 규칙:
--  - number UNIQUE (INV-YYYYMMDD-NNN).
--  - 라인 단가는 SO 라인에서 복사 — 미래 단가 변경에도 회계 금액이 보존된다.
--  - subtotal = SUM(line_total), tax_amount = subtotal × 0.10, total = subtotal + tax.
--  - 발행(ISSUED) 시점에 SO 라인의 invoiced_qty 가 증가하고, SO 헤더 상태가 INVOICING/INVOICED 로 전이.

CREATE TABLE invoice (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    number          VARCHAR(30)     NOT NULL                COMMENT '예: INV-20260524-001',
    sales_order_id  BIGINT          NOT NULL,
    status          VARCHAR(16)     NOT NULL                COMMENT 'DRAFT/ISSUED/CANCELLED',
    invoice_date    DATE            NOT NULL                COMMENT '발행일 (회계 마감의 기준일)',
    subtotal        DECIMAL(15, 2)  NOT NULL DEFAULT 0      COMMENT '공급가액 (부가세 제외)',
    tax_amount      DECIMAL(15, 2)  NOT NULL DEFAULT 0      COMMENT '부가세 (subtotal × 0.10)',
    total_amount    DECIMAL(15, 2)  NOT NULL DEFAULT 0      COMMENT 'subtotal + tax_amount',
    created_at      DATETIME        NOT NULL,
    created_by      VARCHAR(64)     NOT NULL,
    updated_at      DATETIME        NOT NULL,
    updated_by      VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoice_number (number),
    KEY idx_invoice_sales_order (sales_order_id),
    KEY idx_invoice_invoice_date (invoice_date),
    CONSTRAINT fk_invoice_sales_order FOREIGN KEY (sales_order_id) REFERENCES sales_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='인보이스 헤더';

CREATE TABLE invoice_line (
    id                    BIGINT          NOT NULL AUTO_INCREMENT,
    invoice_id            BIGINT          NOT NULL,
    sales_order_line_id   BIGINT          NOT NULL,
    line_no               INT             NOT NULL,
    quantity              DECIMAL(15, 4)  NOT NULL,
    unit_price            DECIMAL(15, 2)  NOT NULL                COMMENT 'SO 라인 단가 복사 (가격 동결)',
    line_total            DECIMAL(15, 2)  NOT NULL                COMMENT 'qty × unit_price (공급가)',
    created_at            DATETIME        NOT NULL,
    created_by            VARCHAR(64)     NOT NULL,
    updated_at            DATETIME        NOT NULL,
    updated_by            VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_invoice_line_invoice (invoice_id),
    KEY idx_invoice_line_sol (sales_order_line_id),
    CONSTRAINT fk_invoice_line_invoice FOREIGN KEY (invoice_id) REFERENCES invoice(id),
    CONSTRAINT fk_invoice_line_sol FOREIGN KEY (sales_order_line_id) REFERENCES sales_order_line(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='인보이스 라인 (SO 라인 단위로 부분 청구)';
