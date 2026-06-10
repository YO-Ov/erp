-- SD↔PP 연계: 계획오더(MRP 제안) 테이블.
-- 수주 확정 시 완제품 부족분(주문량 - 현재고)만큼 PROPOSED 로 자동 생성되고,
-- 생산 담당자가 검토 후 생산지시로 전환(CONVERTED)하거나 기각(DISMISSED)한다.

CREATE TABLE planned_order (
    id                          BIGINT       NOT NULL AUTO_INCREMENT,
    number                      VARCHAR(30)  NOT NULL COMMENT '계획오더 번호 PLO-YYYYMMDD-NNN',
    product_item_id             BIGINT       NOT NULL COMMENT '생산할 완제품',
    required_qty                DECIMAL(15,4) NOT NULL COMMENT '총 필요량 = 수주 주문량(근거)',
    on_hand_qty                 DECIMAL(15,4) NOT NULL COMMENT '생성 시점 현재고(전 창고 합, 근거)',
    shortage_qty                DECIMAL(15,4) NOT NULL COMMENT '부족분 = 생산 제안 수량',
    status                      VARCHAR(16)  NOT NULL COMMENT 'PROPOSED/CONVERTED/DISMISSED',
    source_sales_order_id       BIGINT       NULL     COMMENT '촉발한 수주 id(추적)',
    source_sales_order_number   VARCHAR(30)  NULL     COMMENT '촉발한 수주 번호(추적)',
    converted_production_number VARCHAR(30)  NULL     COMMENT '전환된 생산지시 번호(추적)',
    order_date                  DATE         NOT NULL,
    created_at                  DATETIME     NOT NULL,
    created_by                  VARCHAR(64)  NOT NULL,
    updated_at                  DATETIME     NOT NULL,
    updated_by                  VARCHAR(64)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_planned_order_number UNIQUE (number),
    CONSTRAINT fk_planned_order_product FOREIGN KEY (product_item_id) REFERENCES item (id),
    KEY idx_planned_order_status (status),
    KEY idx_planned_order_source (source_sales_order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '계획오더(MRP 제안)';
