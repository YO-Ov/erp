-- Phase 3: 재고 (Stock) — 상품 × 창고 의 현재 보유량 캐시.
--
-- 비즈니스 규칙:
--  - (item_id, warehouse_id) UNIQUE — 같은 (상품, 창고) 조합은 절대 두 행이 안 됨.
--  - qty_on_hand >= 0 (CHECK 제약) — 음수 재고는 도메인이 막지만 DB 차원에서도 한 번 더.
--  - version 컬럼은 JPA @Version 이 자동으로 증가시킨다 (낙관적 락).
--  - 출고 경로(GoodsIssue.post) 는 추가로 SELECT ... FOR UPDATE 로 비관 락도 함께 사용.
--
-- "현재 재고" 는 캐시. 진실의 원천은 stock_movement 의 누적 SUM.

CREATE TABLE stock (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    item_id       BIGINT          NOT NULL,
    warehouse_id  BIGINT          NOT NULL,
    qty_on_hand   DECIMAL(18, 4)  NOT NULL DEFAULT 0    COMMENT '현재 보유 (캐시)',
    average_cost  DECIMAL(15, 2)  NOT NULL DEFAULT 0    COMMENT '이동평균 원가',
    version       BIGINT          NOT NULL DEFAULT 0    COMMENT 'JPA @Version 낙관적 락',
    created_at    DATETIME        NOT NULL,
    created_by    VARCHAR(64)     NOT NULL,
    updated_at    DATETIME        NOT NULL,
    updated_by    VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_item_warehouse (item_id, warehouse_id),
    KEY idx_stock_warehouse (warehouse_id),
    CONSTRAINT fk_stock_item      FOREIGN KEY (item_id)      REFERENCES item(id),
    CONSTRAINT fk_stock_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    CONSTRAINT chk_stock_qty_non_negative CHECK (qty_on_hand >= 0),
    CONSTRAINT chk_stock_avg_non_negative CHECK (average_cost >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='재고 캐시 ((상품, 창고)당 보유량과 평균 원가)';
