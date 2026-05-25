-- Phase 2: code_sequence 의 year 컬럼을 period_key 로 일반화.
--
-- 배경:
--  - Phase 1: 마스터 코드 (CUST-2026-0001) — 연 단위 발급 (year = 2026).
--  - Phase 2: 트랜잭션 코드 (SO-20260524-001) — 일 단위 발급 (period = '20260524').
--  - 한 테이블로 모두 운영해 비관적 락 패턴을 재사용하기 위해 컬럼을 일반화한다.
--
-- 데이터 보존:
--  - 기존 year=2026 행은 period_key='2026' 으로 자연스럽게 변환된다 (MySQL 의 INT → VARCHAR 자동 변환).
--
-- 제약:
--  - UNIQUE 키 이름도 의미에 맞춰 변경 (uk_code_sequence_prefix_year → uk_code_sequence_prefix_period).

ALTER TABLE code_sequence
    CHANGE COLUMN year period_key VARCHAR(8) NOT NULL COMMENT '발급 단위 — 마스터: YYYY, 트랜잭션: YYYYMMDD';

ALTER TABLE code_sequence
    DROP INDEX uk_code_sequence_prefix_year,
    ADD UNIQUE KEY uk_code_sequence_prefix_period (prefix, period_key);
