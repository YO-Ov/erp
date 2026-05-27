-- Phase 3: 창고 마스터.
--
-- 코드는 자동 생성하지 않고 의미 있는 명시적 값(WH-HQ 등)을 운영자가 직접 부여한다.
-- (Department 와 같은 방침 — 창고는 회사 조직과 1:1, 자주 안 만들고 시퀀스 의미 없음.)

CREATE TABLE warehouse (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(30)  NOT NULL,
    name       VARCHAR(200) NOT NULL                COMMENT '창고명 (본사창고/지방창고 등)',
    address    VARCHAR(500)                         COMMENT '단일 필드 (구조화는 학습 범위 밖)',
    status     VARCHAR(16)  NOT NULL                COMMENT 'ACTIVE/INACTIVE/BLOCKED',
    created_at DATETIME     NOT NULL,
    created_by VARCHAR(64)  NOT NULL,
    updated_at DATETIME     NOT NULL,
    updated_by VARCHAR(64)  NOT NULL,
    deleted_at DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouse_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='창고 마스터 (재고가 실제로 있는 장소)';
