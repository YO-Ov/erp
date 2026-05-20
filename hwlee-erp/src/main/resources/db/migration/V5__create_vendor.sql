-- Phase 1: 거래처 마스터 (매입 — MM 모듈이 참조).
-- Customer 와 거의 동일한 구조이지만 credit_limit 이 없다.

CREATE TABLE vendor (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    code          VARCHAR(30)     NOT NULL,
    name          VARCHAR(200)    NOT NULL                COMMENT '거래처명',
    business_no   VARCHAR(20)     NOT NULL                COMMENT '사업자번호',
    address       VARCHAR(500),
    payment_terms VARCHAR(30)     NOT NULL                COMMENT '우리가 지불할 결제 조건',
    status        VARCHAR(16)     NOT NULL,
    created_at    DATETIME        NOT NULL,
    created_by    VARCHAR(64)     NOT NULL,
    updated_at    DATETIME        NOT NULL,
    updated_by    VARCHAR(64)     NOT NULL,
    deleted_at    DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vendor_code (code),
    UNIQUE KEY uk_vendor_business_no (business_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='거래처 마스터 (MM 모듈이 참조)';
