-- Phase 7: HR 시연 시드 — 인사팀 직원 1명(+로그인 계정/HR 역할) + 기존 직원 급여계약.
--
-- 근태(attendance)는 시연 때 API 로 생성하는 게 학습에 낫다(연장근로가 급여에 반영되는 흐름 체득).
-- 급여계약은 미리 시드해 둬야 급여대장 계산이 바로 돌아간다.
--
-- 시연 계정(비밀번호 pass1234, V28 과 동일 BCrypt 해시):
--   jung@hwlee-erp.example / pass1234 → HR (인사팀)

-- 1) 인사팀 직원
INSERT INTO employee (code, name, email, department_id, hire_date, status,
                      created_at, created_by, updated_at, updated_by, deleted_at)
SELECT CONCAT('EMP-', YEAR(NOW()), '-0005'), '정인사', 'jung@hwlee-erp.example',
       d.id, '2025-03-01', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM department d WHERE d.code = 'DEPT-HR';

UPDATE code_sequence SET next_number = 6 WHERE prefix = 'EMP';

-- 2) 로그인 계정
INSERT INTO app_user (employee_id, username, password_hash, enabled, account_locked,
                      created_at, created_by, updated_at, updated_by)
SELECT e.id, e.email, '$2y$10$fiDYm50/gYzbfsId2MFJqOYpNXfQwoGAX79657DhyryCMTzgbPRoO',
       TRUE, FALSE, NOW(), 'system', NOW(), 'system'
  FROM employee e WHERE e.email = 'jung@hwlee-erp.example';

-- 3) 부서 기반 역할 부여 (DEPT-HR → HR)
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
  FROM app_user u
  JOIN employee e   ON e.id = u.employee_id
  JOIN department d ON d.id = e.department_id
  JOIN role r       ON r.code = 'HR'
 WHERE d.code = 'DEPT-HR';

-- 4) 급여계약 시드 — 4명 모두 2026-01-01 발효, 월 소정근로 209h.
--    (시급 = base_salary / 209. 연장수당은 근태가 있어야 붙는다 → 시연 시 API 로 근태 등록.)
INSERT INTO employment_contract (employee_id, position, base_salary, contracted_hours,
                                 effective_from, effective_to, status,
                                 created_at, created_by, updated_at, updated_by)
SELECT e.id,
       CASE e.email
           WHEN 'kim@hwlee-erp.example'  THEN 'SENIOR'
           WHEN 'lee@hwlee-erp.example'  THEN 'MANAGER'
           WHEN 'park@hwlee-erp.example' THEN 'STAFF'
           WHEN 'jung@hwlee-erp.example' THEN 'SENIOR'
       END,
       CASE e.email
           WHEN 'kim@hwlee-erp.example'  THEN 3500000.00
           WHEN 'lee@hwlee-erp.example'  THEN 4000000.00
           WHEN 'park@hwlee-erp.example' THEN 3000000.00
           WHEN 'jung@hwlee-erp.example' THEN 3300000.00
       END,
       209, '2026-01-01', NULL, 'ACTIVE',
       NOW(), 'system', NOW(), 'system'
  FROM employee e
 WHERE e.email IN ('kim@hwlee-erp.example', 'lee@hwlee-erp.example',
                   'park@hwlee-erp.example', 'jung@hwlee-erp.example');
