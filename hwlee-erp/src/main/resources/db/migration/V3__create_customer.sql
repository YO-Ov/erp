-- Phase 1: 고객 마스터.
--
-- 비즈니스 규칙 (테이블 제약으로 강제):
--  - code 는 유니크 (예: CUST-2026-0001) — 외부 식별자.
--  - business_no 는 유니크 — 같은 회사 중복 등록 방지.
--  - credit_limit 은 0 이상 (Bean Validation 에서도 검증).
--
-- Soft Delete:
--  - 물리 삭제 대신 deleted_at 에 시각을 기록 (NULL=살아있음).
--  - 일반 조회는 @SQLRestriction("deleted_at IS NULL") 으로 자동 필터.
--
-- 인덱스 결정:
--  - code: UNIQUE (단건 조회 + 외부 식별자)
--  - business_no: UNIQUE (중복 방지 + 사업자번호 검색)
--  - name: LIKE '%키워드%' 검색 패턴이라 인덱스 효과 낮음 → 인덱스 생략
--  - status: 카디널리티 낮음 → 인덱스 생략 (필요 시 Phase 후반에 보강)

CREATE TABLE customer (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    code          VARCHAR(30)     NOT NULL,
    name          VARCHAR(200)    NOT NULL                 COMMENT '회사명',
    business_no   VARCHAR(20)     NOT NULL                 COMMENT '사업자번호 (예: 123-45-67890)',
    address       VARCHAR(500)                             COMMENT '주소 (구조화 없이 단일 필드)',
    credit_limit  DECIMAL(15, 2)  NOT NULL DEFAULT 0       COMMENT '신용한도 — Phase 2 수주 시 체크',
    payment_terms VARCHAR(30)     NOT NULL                 COMMENT 'NET30/NET60/COD/PREPAID',
    status        VARCHAR(16)     NOT NULL                 COMMENT 'ACTIVE/INACTIVE/BLOCKED',
    created_at    DATETIME        NOT NULL,
    created_by    VARCHAR(64)     NOT NULL,
    updated_at    DATETIME        NOT NULL,
    updated_by    VARCHAR(64)     NOT NULL,
    deleted_at    DATETIME                                 COMMENT 'NULL=살아있음, 값=Soft Delete 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_code (code),
    UNIQUE KEY uk_customer_business_no (business_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='고객 마스터 (SD 모듈이 참조)';
