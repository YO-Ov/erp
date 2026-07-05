-- 전자결재 엔진: 결재 문서(approval_request) + 결재선 단계(approval_step) + 전결 규정(approval_rule).
-- 범용 엔진 — 견적을 시작으로 여러 업무 문서가 이 위에 얹힌다.

-- 1) 결재 문서.
CREATE TABLE approval_request (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    number        VARCHAR(30)   NOT NULL COMMENT '결재 번호 APV-YYYYMMDD-NNN',
    doc_type      VARCHAR(24)   NOT NULL COMMENT '문서 종류 QUOTATION/SALES_ORDER/...',
    ref_id        BIGINT        NOT NULL COMMENT '원본 문서 id',
    ref_no        VARCHAR(30)   NULL     COMMENT '원본 문서 번호(스냅샷)',
    title         VARCHAR(200)  NOT NULL,
    amount        DECIMAL(15,2) NOT NULL COMMENT '전결 규정 판정 금액',
    status        VARCHAR(16)   NOT NULL COMMENT 'DRAFT/PENDING/APPROVED/REJECTED/WITHDRAWN',
    current_step  INT           NOT NULL DEFAULT 0 COMMENT '현재 순차 결재 단계 stepNo',
    requested_at  DATETIME      NULL,
    decided_at    DATETIME      NULL,
    return_reason VARCHAR(500)  NULL,
    created_at    DATETIME      NOT NULL,
    created_by    VARCHAR(64)   NOT NULL COMMENT '상신자',
    updated_at    DATETIME      NOT NULL,
    updated_by    VARCHAR(64)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_approval_request_number UNIQUE (number),
    KEY idx_approval_request_status (status),
    KEY idx_approval_request_doc (doc_type, ref_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '전자결재 문서';

-- 2) 결재선 단계.
CREATE TABLE approval_step (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    approval_id   BIGINT       NOT NULL,
    step_no       INT          NOT NULL COMMENT '결재선 순번(결재는 순차 진행 기준)',
    step_type     VARCHAR(16)  NOT NULL COMMENT 'APPROVAL(결재)/AGREEMENT(합의)/REFERENCE(참조)',
    approver      VARCHAR(64)  NOT NULL COMMENT '결재자 username',
    approver_name VARCHAR(60)  NULL     COMMENT '결재자 이름 스냅샷',
    dept_name     VARCHAR(100) NULL     COMMENT '결재자 소속/직책 스냅샷',
    status        VARCHAR(16)  NOT NULL COMMENT 'PENDING/APPROVED/REJECTED/SKIPPED',
    decided_at    DATETIME     NULL,
    comment       VARCHAR(500) NULL,
    created_at    DATETIME     NOT NULL,
    created_by    VARCHAR(64)  NOT NULL,
    updated_at    DATETIME     NOT NULL,
    updated_by    VARCHAR(64)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_approval_step_request FOREIGN KEY (approval_id) REFERENCES approval_request (id),
    KEY idx_approval_step_request (approval_id),
    KEY idx_approval_step_approver (approver, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '전자결재 결재선 단계';

-- 3) 전결 규정.
CREATE TABLE approval_rule (
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    doc_type                  VARCHAR(24)   NOT NULL,
    min_amount                DECIMAL(15,2) NOT NULL COMMENT '구간 하한(이상)',
    max_amount                DECIMAL(15,2) NULL     COMMENT '구간 상한(미만). NULL=무한대',
    approval_level            VARCHAR(16)   NOT NULL COMMENT 'TEAM/DIVISION/COMPANY',
    require_finance_agreement TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '재무 합의 필요 여부',
    created_at                DATETIME      NOT NULL,
    created_by                VARCHAR(64)   NOT NULL,
    updated_at                DATETIME      NOT NULL,
    updated_by                VARCHAR(64)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_approval_rule_doc (doc_type, min_amount)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '전자결재 전결 규정';

-- 4) 견적 전결 규정 시드 — 금액 구간별 결재 레벨.
--    1천만 미만: 팀장 전결 / 1천만~5천만: 본부장까지 / 5천만 이상: 대표까지 + 재무 합의.
INSERT INTO approval_rule (doc_type, min_amount, max_amount, approval_level, require_finance_agreement, created_at, created_by, updated_at, updated_by) VALUES
    ('QUOTATION',        0.00, 10000000.00, 'TEAM',     0, NOW(), 'system', NOW(), 'system'),
    ('QUOTATION', 10000000.00, 50000000.00, 'DIVISION', 0, NOW(), 'system', NOW(), 'system'),
    ('QUOTATION', 50000000.00, NULL,        'COMPANY',  1, NOW(), 'system', NOW(), 'system');
