-- Phase 1: 마스터 비즈니스 코드(CUST-2026-0001 등) 발급을 위한 시퀀스 테이블.
--
-- 동시성:
--  - 발급 시 (prefix, year) 행을 SELECT ... FOR UPDATE 로 점유한다.
--  - 같은 (prefix, year) 를 동시에 요청한 두 트랜잭션은 줄을 서서 처리된다.
--  - 트랜잭션이 롤백되어도 next_number 는 이미 커밋되어 "번호 구멍" 이 생길 수 있다.
--    (REQUIRES_NEW 트랜잭션으로 발급하므로 호출 측 롤백과 무관하게 번호가 소진된다.)
--    학습용으로 이 트레이드오프를 수용한다.
--
-- 인덱스:
--  - PRIMARY KEY (id)
--  - UNIQUE INDEX (prefix, year) — (prefix, year) 조합이 유일해야 발급 로직이 동작한다.

CREATE TABLE code_sequence (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    prefix      VARCHAR(16)  NOT NULL COMMENT '코드 접두어 (CUST/ITEM/VEND/EMP 등)',
    year        INT          NOT NULL COMMENT '발급 연도 (4자리)',
    next_number INT          NOT NULL COMMENT '다음에 발급할 일련번호 (1부터 시작)',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_sequence_prefix_year (prefix, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='마스터 비즈니스 코드 발급 시퀀스';
