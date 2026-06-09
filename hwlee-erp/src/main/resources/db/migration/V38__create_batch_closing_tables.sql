-- Phase 9: 야간/월말 마감 배치의 결과(스냅샷) 테이블 3종.
--
-- 공통 설계 — 멱등 재실행:
--   각 테이블은 기준일(+키)에 UNIQUE 제약을 둔다. 배치는 실행 시 "해당 기준일 행을 모두 삭제 후
--   재삽입"하므로, 같은 날짜로 몇 번을 다시 돌려도 결과가 동일하다(re-runnable). 이는 배치 실패 후
--   재실행/수동 재집계의 안전성을 보장하는 기간계의 기본 패턴이다.

-- 1) 일일 매출 마감: 기준일의 ISSUED 인보이스를 합산한 1행.
CREATE TABLE daily_sales_closing (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    closing_date  DATE           NOT NULL                COMMENT '마감 기준일',
    invoice_count INT            NOT NULL                COMMENT '집계된 ISSUED 인보이스 건수',
    subtotal      DECIMAL(15, 2) NOT NULL                COMMENT '공급가 합계',
    tax_amount    DECIMAL(15, 2) NOT NULL                COMMENT '부가세 합계',
    total_amount  DECIMAL(15, 2) NOT NULL                COMMENT '합계(공급가+세액)',
    closed_at     DATETIME       NOT NULL                COMMENT '마감 처리 시각',
    created_at    DATETIME       NOT NULL,
    created_by    VARCHAR(64)    NOT NULL,
    updated_at    DATETIME       NOT NULL,
    updated_by    VARCHAR(64)    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_sales_closing_date (closing_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='일일 매출 마감 스냅샷';

-- 2) 재고 평가: 월말 기준일의 (품목, 창고)별 평가액 스냅샷.
--    평가액 = qty_on_hand × average_cost (가중평균법). 진실의 원천인 stock 의 그 시점 캐시를 박제.
CREATE TABLE inventory_valuation (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    valuation_date    DATE           NOT NULL                COMMENT '평가 기준일(보통 월말일)',
    item_id           BIGINT         NOT NULL,
    warehouse_id      BIGINT         NOT NULL,
    qty_on_hand       DECIMAL(18, 4) NOT NULL                COMMENT '평가 시점 보유 수량',
    average_cost      DECIMAL(15, 2) NOT NULL                COMMENT '평가 시점 평균 단가',
    valuation_amount  DECIMAL(18, 2) NOT NULL                COMMENT '평가액 = 수량 × 평균단가',
    created_at        DATETIME       NOT NULL,
    created_by        VARCHAR(64)    NOT NULL,
    updated_at        DATETIME       NOT NULL,
    updated_by        VARCHAR(64)    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_val_date_item_wh (valuation_date, item_id, warehouse_id),
    KEY idx_inv_val_date (valuation_date),
    CONSTRAINT fk_inv_val_item      FOREIGN KEY (item_id)      REFERENCES item(id),
    CONSTRAINT fk_inv_val_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='재고 평가 스냅샷(월말 결산)';

-- 3) 채권 노령화: 월말 기준일의 고객별 미수금을 연령 버킷으로 분류.
--    미수금 = SUM(ISSUED 인보이스 합계) - SUM(RECEIPT 입금액). 인보이스일 기준 경과일로 버킷 배정.
--    별도 Receivable 엔티티가 없으므로 인보이스/입금에서 파생 집계한다(학습용 간이 AR Aging).
CREATE TABLE ar_aging (
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    aging_date         DATE           NOT NULL                COMMENT '노령화 기준일',
    customer_id        BIGINT         NOT NULL,
    bucket_0_30        DECIMAL(15, 2) NOT NULL                COMMENT '경과 0~30일 미수',
    bucket_31_60       DECIMAL(15, 2) NOT NULL                COMMENT '경과 31~60일 미수',
    bucket_61_90       DECIMAL(15, 2) NOT NULL                COMMENT '경과 61~90일 미수',
    bucket_over_90     DECIMAL(15, 2) NOT NULL                COMMENT '경과 91일 이상 미수',
    total_outstanding  DECIMAL(15, 2) NOT NULL                COMMENT '미수 총액(버킷 합)',
    created_at         DATETIME       NOT NULL,
    created_by         VARCHAR(64)    NOT NULL,
    updated_at         DATETIME       NOT NULL,
    updated_by         VARCHAR(64)    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ar_aging_date_customer (aging_date, customer_id),
    KEY idx_ar_aging_date (aging_date),
    CONSTRAINT fk_ar_aging_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='채권 노령화 스냅샷(월말 결산)';
