-- Phase 1: 시연/개발용 시드 데이터.
-- 마스터만 시드한다 (트랜잭션 데이터는 절대 시드하지 않음).
--
-- 부서 구조:
--   현우전자
--   ├─ 영업팀 (DEPT-SALES)
--   ├─ 구매팀 (DEPT-PURCHASE)
--   ├─ 생산팀 (DEPT-PRODUCTION)
--   ├─ 재무팀 (DEPT-FINANCE)
--   └─ 인사팀 (DEPT-HR)

-- 1) 루트 부서 (회사)
INSERT INTO department (code, name, parent_id, status, created_at, created_by, updated_at, updated_by, deleted_at)
VALUES ('DEPT-HQ', '현우전자', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL);

SET @hq_id = LAST_INSERT_ID();

-- 2) 하위 부서
INSERT INTO department (code, name, parent_id, status, created_at, created_by, updated_at, updated_by, deleted_at) VALUES
    ('DEPT-SALES',      '영업팀', @hq_id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('DEPT-PURCHASE',   '구매팀', @hq_id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('DEPT-PRODUCTION', '생산팀', @hq_id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('DEPT-FINANCE',    '재무팀', @hq_id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('DEPT-HR',         '인사팀', @hq_id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL);

-- 3) 코드 시퀀스 초기화 — 시드된 직원 다음 번호부터 발급되도록.
INSERT INTO code_sequence (prefix, year, next_number, updated_at) VALUES
    ('CUST', YEAR(NOW()), 3, NOW()),
    ('ITEM', YEAR(NOW()), 3, NOW()),
    ('VEND', YEAR(NOW()), 2, NOW()),
    ('EMP',  YEAR(NOW()), 4, NOW());

-- 4) 시연용 고객 2건
INSERT INTO customer (code, name, business_no, address, credit_limit, payment_terms, status,
                      created_at, created_by, updated_at, updated_by, deleted_at) VALUES
    (CONCAT('CUST-', YEAR(NOW()), '-0001'), '신원전자', '111-22-33333', '서울시 강남구',
     100000000.00, 'NET30', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    (CONCAT('CUST-', YEAR(NOW()), '-0002'), '한솔테크', '444-55-66666', '경기도 성남시',
      50000000.00, 'NET60', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL);

-- 5) 시연용 상품 2건 (노트북 + 모니터)
INSERT INTO item (code, name, category, unit, standard_cost, standard_price, status,
                  created_at, created_by, updated_at, updated_by, deleted_at) VALUES
    (CONCAT('ITEM-', YEAR(NOW()), '-0001'), '현우 노트북 15"',  'NOTEBOOK', 'EA',
      800000.00, 1200000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    (CONCAT('ITEM-', YEAR(NOW()), '-0002'), '현우 모니터 27"',  'MONITOR',  'EA',
      200000.00,  350000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL);

-- 6) 시연용 거래처 1건
INSERT INTO vendor (code, name, business_no, address, payment_terms, status,
                    created_at, created_by, updated_at, updated_by, deleted_at) VALUES
    (CONCAT('VEND-', YEAR(NOW()), '-0001'), '동부부품', '777-88-99999', '인천시',
     'NET30', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL);

-- 7) 시연용 직원 3건 (영업/재무/생산 각 1명)
INSERT INTO employee (code, name, email, department_id, hire_date, status,
                      created_at, created_by, updated_at, updated_by, deleted_at)
SELECT CONCAT('EMP-', YEAR(NOW()), '-0001'), '김영업', 'kim@hwlee-erp.example',
       d.id, '2025-01-15', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM department d WHERE d.code = 'DEPT-SALES';

INSERT INTO employee (code, name, email, department_id, hire_date, status,
                      created_at, created_by, updated_at, updated_by, deleted_at)
SELECT CONCAT('EMP-', YEAR(NOW()), '-0002'), '이재무', 'lee@hwlee-erp.example',
       d.id, '2024-06-01', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM department d WHERE d.code = 'DEPT-FINANCE';

INSERT INTO employee (code, name, email, department_id, hire_date, status,
                      created_at, created_by, updated_at, updated_by, deleted_at)
SELECT CONCAT('EMP-', YEAR(NOW()), '-0003'), '박생산', 'park@hwlee-erp.example',
       d.id, '2026-02-20', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM department d WHERE d.code = 'DEPT-PRODUCTION';
