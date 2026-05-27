-- Phase 3: 재고 이동 원장 (Stock Movement) — append-only.
--
-- 비즈니스 규칙:
--  - 이 테이블의 행은 절대 수정/삭제되지 않는다 (감사 원장).
--  - qty_delta 의 부호 하나가 입고(+)/출고(-)/조정 모두를 표현.
--  - reason 의 부호와 qty_delta 의 부호는 일치해야 한다 (도메인이 강제).
--  - ref_type/ref_id 는 약한 참조 (FK 아님) — 새 이동 유형이 추가돼도 컬럼 안 늘어남.
--
-- 정합성 (테스트/배치로 검증):
--   SUM(stock_movement.qty_delta) WHERE (item, warehouse) = stock.qty_on_hand

CREATE TABLE stock_movement (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    item_id       BIGINT          NOT NULL,
    warehouse_id  BIGINT          NOT NULL,
    qty_delta     DECIMAL(18, 4)  NOT NULL                COMMENT '+ 입고 / - 출고',
    unit_cost     DECIMAL(15, 2)  NOT NULL                COMMENT '이 이동의 단가 (입고:매입단가, 출고:직전 평균)',
    reason        VARCHAR(20)     NOT NULL                COMMENT 'GOODS_RECEIPT/GOODS_ISSUE/ADJUSTMENT_PLUS/ADJUSTMENT_MINUS/SCRAP',
    ref_type      VARCHAR(10)                             COMMENT 'GR/GI/ADJ (선택)',
    ref_id        BIGINT                                  COMMENT '트랜잭션 ID (약한 참조)',
    moved_at      DATETIME        NOT NULL                COMMENT '이동 일시',
    created_at    DATETIME        NOT NULL,
    created_by    VARCHAR(64)     NOT NULL,
    updated_at    DATETIME        NOT NULL,
    updated_by    VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_stock_movement_item_wh_time (item_id, warehouse_id, moved_at),
    KEY idx_stock_movement_ref (ref_type, ref_id),
    CONSTRAINT fk_stock_movement_item      FOREIGN KEY (item_id)      REFERENCES item(id),
    CONSTRAINT fk_stock_movement_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    CONSTRAINT chk_stock_movement_delta_nonzero CHECK (qty_delta <> 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='재고 이동 원장 (append-only)';
