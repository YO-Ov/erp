-- Phase 12: MES 도메인 — 설비/작업자 마스터 + 작업지시(ERP 수신).

CREATE TABLE equipment (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(30)  NOT NULL                COMMENT '설비코드 EQ-XXX',
    name       VARCHAR(100) NOT NULL,
    line_name  VARCHAR(100)                         COMMENT '소속 라인',
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_equipment_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='설비 마스터';

CREATE TABLE operator (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(30)  NOT NULL                COMMENT '작업자코드 OP-XXX',
    name       VARCHAR(100) NOT NULL,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_operator_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='작업자 마스터';

-- 작업지시: ERP 생산지시(PO)가 MES 로 내려온 것.
-- erp_order_no UNIQUE = 멱등 수신 키(같은 PO 가 두 번 와도 한 번만 등록).
CREATE TABLE work_order (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    work_order_no VARCHAR(30)    NOT NULL                COMMENT 'MES 작업지시번호 WO-XXX',
    erp_order_no  VARCHAR(30)    NOT NULL                COMMENT 'ERP 생산지시번호 PO-XXX (멱등키)',
    product_code  VARCHAR(30)    NOT NULL,
    product_name  VARCHAR(100)   NOT NULL,
    quantity      DECIMAL(18, 4) NOT NULL,
    planned_date  DATE,
    status        VARCHAR(16)    NOT NULL                COMMENT 'RECEIVED/IN_PROGRESS/COMPLETED/CANCELLED',
    received_at   DATETIME       NOT NULL,
    created_at    DATETIME       NOT NULL,
    updated_at    DATETIME       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_order_no (work_order_no),
    UNIQUE KEY uk_work_order_erp_no (erp_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='작업지시(ERP 수신)';

CREATE TABLE work_order_line (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    work_order_id  BIGINT         NOT NULL,
    line_no        INT            NOT NULL,
    component_code VARCHAR(30)    NOT NULL,
    component_name VARCHAR(100)   NOT NULL,
    required_qty   DECIMAL(18, 4) NOT NULL,
    unit           VARCHAR(20),
    created_at     DATETIME       NOT NULL,
    updated_at     DATETIME       NOT NULL,
    PRIMARY KEY (id),
    KEY idx_wol_work_order (work_order_id),
    CONSTRAINT fk_wol_work_order FOREIGN KEY (work_order_id) REFERENCES work_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='작업지시 소요자재 라인';

-- 마스터 시드 (현장 설비/작업자)
INSERT INTO equipment (code, name, line_name, created_at, updated_at) VALUES
    ('EQ-001', '노트북 조립 라인 #1', 'ASSEMBLY-1', NOW(), NOW()),
    ('EQ-002', '검사·포장 라인',      'PACKAGING',  NOW(), NOW());

INSERT INTO operator (code, name, created_at, updated_at) VALUES
    ('OP-001', '김현장', NOW(), NOW()),
    ('OP-002', '이라인', NOW(), NOW());
