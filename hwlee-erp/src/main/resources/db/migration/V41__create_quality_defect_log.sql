-- Phase 15: MES 불량 통보 기록(통계용). ERP 는 불량을 재고/회계에 반영하지 않고 기록만 한다.
-- event_id UNIQUE 로 중복 수신 멱등 처리.

CREATE TABLE quality_defect_log (
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    event_id           VARCHAR(80)    NOT NULL                COMMENT '멱등 키',
    work_order_no      VARCHAR(30)    NOT NULL,
    erp_order_no       VARCHAR(30)    NOT NULL,
    product_code       VARCHAR(30)    NOT NULL,
    defect_qty         DECIMAL(18, 4) NOT NULL,
    defect_reason_code VARCHAR(30),
    defect_reason_name VARCHAR(100),
    inspected_at       DATETIME,
    received_at        DATETIME       NOT NULL,
    created_at         DATETIME       NOT NULL,
    created_by         VARCHAR(64)    NOT NULL,
    updated_at         DATETIME       NOT NULL,
    updated_by         VARCHAR(64)    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_qdl_event (event_id),
    KEY idx_qdl_erp_order (erp_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES 불량 통보 기록';
