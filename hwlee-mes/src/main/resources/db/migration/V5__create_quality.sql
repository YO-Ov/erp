-- Phase 15: 품질 — 불량 사유 마스터 + 품질 검사.

CREATE TABLE defect_reason (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(30)  NOT NULL                COMMENT '불량 코드 DEF-XX',
    name       VARCHAR(100) NOT NULL,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_defect_reason_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='불량 사유 마스터';

CREATE TABLE quality_inspection (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    work_order_id    BIGINT         NOT NULL,
    inspected_qty    DECIMAL(18, 4) NOT NULL                COMMENT '검사 수량',
    passed_qty       DECIMAL(18, 4) NOT NULL                COMMENT '합격 수량',
    defect_qty       DECIMAL(18, 4) NOT NULL                COMMENT '불량 수량',
    defect_reason_id BIGINT                                 COMMENT '불량 사유(불량 있을 때)',
    result           VARCHAR(16)    NOT NULL                COMMENT 'PASS/FAIL',
    inspected_at     DATETIME       NOT NULL,
    note             VARCHAR(255),
    created_at       DATETIME       NOT NULL,
    updated_at       DATETIME       NOT NULL,
    PRIMARY KEY (id),
    KEY idx_qi_work_order (work_order_id),
    CONSTRAINT fk_qi_work_order FOREIGN KEY (work_order_id) REFERENCES work_order(id),
    CONSTRAINT fk_qi_defect_reason FOREIGN KEY (defect_reason_id) REFERENCES defect_reason(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='품질 검사';

INSERT INTO defect_reason (code, name, created_at, updated_at) VALUES
    ('DEF-01', '외관 불량(스크래치/오염)', NOW(), NOW()),
    ('DEF-02', '기능 불량(동작 이상)',     NOW(), NOW()),
    ('DEF-03', '조립 불량(체결 불량)',     NOW(), NOW());
