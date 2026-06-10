-- 여신(신용한도) 상향 요청: 영업이 제출(PENDING) → 재무가 승인(APPROVED, 고객 한도 반영)/거부(REJECTED).

CREATE TABLE credit_limit_request (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    number          VARCHAR(30)  NOT NULL COMMENT '요청 번호 CLR-YYYYMMDD-NNN',
    customer_id     BIGINT       NOT NULL,
    current_limit   DECIMAL(15,2) NOT NULL COMMENT '요청 시점 현재 한도(근거)',
    requested_limit DECIMAL(15,2) NOT NULL COMMENT '요청 목표 한도',
    reason          VARCHAR(500) NOT NULL COMMENT '상향 사유',
    status          VARCHAR(16)  NOT NULL COMMENT 'PENDING/APPROVED/REJECTED',
    decided_by      VARCHAR(64)  NULL     COMMENT '결정한 재무 담당자(username)',
    decided_at      DATETIME     NULL,
    decision_note   VARCHAR(500) NULL,
    created_at      DATETIME     NOT NULL,
    created_by      VARCHAR(64)  NOT NULL COMMENT '요청자(영업)',
    updated_at      DATETIME     NOT NULL,
    updated_by      VARCHAR(64)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_credit_limit_request_number UNIQUE (number),
    CONSTRAINT fk_credit_limit_request_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    KEY idx_credit_limit_request_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '여신 상향 요청';
