-- 고객 담당자(연락처) — 한 고객에 여러 담당자(구매담당·경리담당·현장담당 등).
--
-- 비즈니스 규칙:
--  - 한 고객(customer_id)에 담당자 N명 (1:N).
--  - is_primary 는 대표 담당자 표시 — "고객당 1명" 은 애플리케이션(도메인)에서 보장한다.
--  - 담당자는 이력 보존 대상이 아니라 물리 삭제(orphanRemoval). Soft Delete 없음.

CREATE TABLE customer_contact (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    customer_id BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL                COMMENT '담당자명',
    position    VARCHAR(100)                         COMMENT '부서·직책 (예: 구매팀 과장)',
    phone       VARCHAR(30)                          COMMENT '연락처',
    email       VARCHAR(200)                         COMMENT '이메일',
    is_primary  BOOLEAN      NOT NULL DEFAULT FALSE  COMMENT '대표 담당자 여부 (고객당 1명)',
    created_at  DATETIME     NOT NULL,
    created_by  VARCHAR(64)  NOT NULL,
    updated_at  DATETIME     NOT NULL,
    updated_by  VARCHAR(64)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_customer_contact_customer (customer_id),
    CONSTRAINT fk_customer_contact_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='고객 담당자(연락처)';
