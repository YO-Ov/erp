-- Phase 1: 부서 마스터 (자기참조 트리).
--
-- 코드는 자동 생성하지 않고 의미 있는 명시적 값(DEPT-SALES 등)을 운영자가 직접 부여한다.
-- parent_id 는 NULL 가능 (루트 부서). 트리 깊이는 Phase 1 에서는 2단계만 가정.

CREATE TABLE department (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(30)  NOT NULL,
    name       VARCHAR(100) NOT NULL                COMMENT '부서명 (영업팀/재무팀 등)',
    parent_id  BIGINT                               COMMENT '상위 부서 id (NULL=루트)',
    status     VARCHAR(16)  NOT NULL,
    created_at DATETIME     NOT NULL,
    created_by VARCHAR(64)  NOT NULL,
    updated_at DATETIME     NOT NULL,
    updated_by VARCHAR(64)  NOT NULL,
    deleted_at DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_department_code (code),
    KEY idx_department_parent_id (parent_id),
    CONSTRAINT fk_department_parent FOREIGN KEY (parent_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='부서 마스터 (자기참조 트리)';
