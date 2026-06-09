-- Phase 13: 현장 실행 — 작업지시 실행 컬럼 + 생산 실적 + 자재 투입.

ALTER TABLE work_order
    ADD COLUMN assigned_equipment_id BIGINT        NULL           AFTER received_at,
    ADD COLUMN assigned_operator_id  BIGINT        NULL           AFTER assigned_equipment_id,
    ADD COLUMN produced_qty          DECIMAL(18,4) NOT NULL DEFAULT 0 AFTER assigned_operator_id,
    ADD COLUMN defect_qty            DECIMAL(18,4) NOT NULL DEFAULT 0 AFTER produced_qty,
    ADD COLUMN started_at            DATETIME      NULL           AFTER defect_qty,
    ADD COLUMN finished_at           DATETIME      NULL           AFTER started_at,
    ADD CONSTRAINT fk_wo_equipment FOREIGN KEY (assigned_equipment_id) REFERENCES equipment(id),
    ADD CONSTRAINT fk_wo_operator  FOREIGN KEY (assigned_operator_id)  REFERENCES operator(id);

-- 생산 실적 (부분 실적 — 한 작업지시에 여러 건)
CREATE TABLE production_result (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    work_order_id BIGINT         NOT NULL,
    seq           INT            NOT NULL                COMMENT '작업지시 내 실적 순번',
    good_qty      DECIMAL(18, 4) NOT NULL                COMMENT '양품 수량',
    defect_qty    DECIMAL(18, 4) NOT NULL                COMMENT '불량 수량',
    reported_at   DATETIME       NOT NULL,
    note          VARCHAR(255),
    created_at    DATETIME       NOT NULL,
    updated_at    DATETIME       NOT NULL,
    PRIMARY KEY (id),
    KEY idx_pr_work_order (work_order_id),
    CONSTRAINT fk_pr_work_order FOREIGN KEY (work_order_id) REFERENCES work_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='생산 실적';

-- 자재 투입 (BOM 비례 × 실적 양품수량)
CREATE TABLE material_consumption (
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    production_result_id BIGINT         NOT NULL,
    component_code       VARCHAR(30)    NOT NULL,
    component_name       VARCHAR(100)   NOT NULL,
    consumed_qty         DECIMAL(18, 4) NOT NULL,
    created_at           DATETIME       NOT NULL,
    updated_at           DATETIME       NOT NULL,
    PRIMARY KEY (id),
    KEY idx_mc_result (production_result_id),
    CONSTRAINT fk_mc_result FOREIGN KEY (production_result_id) REFERENCES production_result(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='자재 투입';
