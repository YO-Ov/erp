-- Phase 1: 상품 마스터.
--
-- 모든 ERP 모듈이 참조하는 핵심 마스터:
--   - SD: 수주 라인의 상품
--   - MM: 재고/입출고 단위
--   - FI: 매출 원가 계산 기준
--   - PP: BOM 의 노드
--
-- 검증 정책:
--  - standard_price >= standard_cost 는 Phase 5 에서 결정 (실무에서는 손해 판매도 있음).
--  - 단가는 모두 DECIMAL — float/double 은 회계 오차를 만들기 때문에 금지.

CREATE TABLE item (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    code           VARCHAR(30)     NOT NULL,
    name           VARCHAR(200)    NOT NULL                COMMENT '상품명',
    category       VARCHAR(20)     NOT NULL                COMMENT 'NOTEBOOK/MONITOR',
    unit           VARCHAR(10)     NOT NULL                COMMENT 'EA/BOX/KG',
    standard_cost  DECIMAL(15, 2)  NOT NULL                COMMENT '표준 원가',
    standard_price DECIMAL(15, 2)  NOT NULL                COMMENT '표준 판매가',
    status         VARCHAR(16)     NOT NULL                COMMENT 'ACTIVE/INACTIVE/BLOCKED',
    created_at     DATETIME        NOT NULL,
    created_by     VARCHAR(64)     NOT NULL,
    updated_at     DATETIME        NOT NULL,
    updated_by     VARCHAR(64)     NOT NULL,
    deleted_at     DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_item_code (code),
    KEY idx_item_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='상품 마스터 (전 모듈 참조)';
