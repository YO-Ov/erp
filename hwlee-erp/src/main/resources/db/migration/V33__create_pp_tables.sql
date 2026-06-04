-- Phase 8: PP(생산) 스키마 — Item 보강 + BOM + 생산지시(헤더-라인).

-- 1) Item 에 완제품/부품 구분 추가. 기존 품목은 모두 완제품(FINISHED).
ALTER TABLE item
    ADD COLUMN item_type VARCHAR(16) NOT NULL DEFAULT 'FINISHED' COMMENT 'FINISHED/COMPONENT (Phase 8)' AFTER category;

-- 2) BOM — 완제품(부모) ↔ 부품(자식) 자기참조, 완제품 1개당 소요량(단일 레벨).
CREATE TABLE bom (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    product_item_id   BIGINT          NOT NULL                COMMENT '완제품 (FINISHED)',
    component_item_id BIGINT          NOT NULL                COMMENT '부품 (COMPONENT)',
    quantity          DECIMAL(15, 2)  NOT NULL                COMMENT '완제품 1개당 소요량',
    created_at        DATETIME        NOT NULL,
    created_by        VARCHAR(64)     NOT NULL,
    updated_at        DATETIME        NOT NULL,
    updated_by        VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bom_product_component (product_item_id, component_item_id),
    KEY idx_bom_product (product_item_id),
    CONSTRAINT fk_bom_product   FOREIGN KEY (product_item_id)   REFERENCES item(id),
    CONSTRAINT fk_bom_component FOREIGN KEY (component_item_id) REFERENCES item(id),
    CONSTRAINT chk_bom_qty CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM (자재 명세서, 단일 레벨)';

-- 3) 생산지시 헤더.
CREATE TABLE production_order (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    number          VARCHAR(30)     NOT NULL                COMMENT '예: PO-20260604-001',
    product_item_id BIGINT          NOT NULL                COMMENT '만들 완제품',
    warehouse_id    BIGINT          NOT NULL                COMMENT '부품 출고·완제품 입고 창고',
    quantity        DECIMAL(15, 2)  NOT NULL                COMMENT '생산 수량',
    status          VARCHAR(16)     NOT NULL                COMMENT 'PLANNED/RELEASED/COMPLETED/CANCELLED',
    order_date      DATE            NOT NULL,
    due_date        DATE,
    completed_at    DATETIME,
    created_at      DATETIME        NOT NULL,
    created_by      VARCHAR(64)     NOT NULL,
    updated_at      DATETIME        NOT NULL,
    updated_by      VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_production_order_number (number),
    KEY idx_production_order_product (product_item_id),
    KEY idx_production_order_status (status),
    KEY idx_production_order_date (order_date),
    CONSTRAINT fk_production_order_product   FOREIGN KEY (product_item_id) REFERENCES item(id),
    CONSTRAINT fk_production_order_warehouse FOREIGN KEY (warehouse_id)    REFERENCES warehouse(id),
    CONSTRAINT chk_production_order_qty CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='생산지시 헤더';

-- 4) 생산지시 소요 자재 라인 (BOM×수량 스냅샷).
CREATE TABLE production_order_line (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    production_order_id BIGINT          NOT NULL,
    line_no             INT             NOT NULL,
    component_item_id   BIGINT          NOT NULL                COMMENT '투입 부품',
    required_qty        DECIMAL(15, 2)  NOT NULL                COMMENT '소요량 = BOM × 생산수량',
    issued_unit_cost    DECIMAL(15, 2)                          COMMENT '완료 시 실제 출고 이동평균 단가 (완료 전 NULL)',
    created_at          DATETIME        NOT NULL,
    created_by          VARCHAR(64)     NOT NULL,
    updated_at          DATETIME        NOT NULL,
    updated_by          VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_production_order_line_header (production_order_id),
    KEY idx_production_order_line_item (component_item_id),
    CONSTRAINT fk_pol_header    FOREIGN KEY (production_order_id) REFERENCES production_order(id),
    CONSTRAINT fk_pol_component FOREIGN KEY (component_item_id)   REFERENCES item(id),
    CONSTRAINT chk_pol_required_qty CHECK (required_qty > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='생산지시 소요 자재 라인';
