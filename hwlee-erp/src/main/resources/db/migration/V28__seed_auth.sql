-- V28: 인증/인가 시드 (Phase 6)
-- 역할/권한 + role_permission + 기존 직원→app_user(BCrypt) + 부서 기반 user_role + admin 계정.
--
-- 시연용 비밀번호는 전 계정 동일: "pass1234" (BCrypt 해시로 저장 — 평문 금지 원칙).
-- 해시: $2y$10$fiDYm50/gYzbfsId2MFJqOYpNXfQwoGAX79657DhyryCMTzgbPRoO  (= pass1234)

-- 1) 역할
INSERT INTO role (code, name, created_at, created_by, updated_at, updated_by) VALUES
    ('SALES',      '영업',   NOW(), 'system', NOW(), 'system'),
    ('PURCHASING', '구매',   NOW(), 'system', NOW(), 'system'),
    ('FINANCE',    '재무',   NOW(), 'system', NOW(), 'system'),
    ('ADMIN',      '관리자', NOW(), 'system', NOW(), 'system');

-- 2) 권한 (이번 Phase 는 역할 단위 인가 — permission 은 구조+시드만, 추후 세분화 복선)
INSERT INTO permission (code, name, created_at, created_by, updated_at, updated_by) VALUES
    ('SD_READ',     'SD 조회',   NOW(), 'system', NOW(), 'system'),
    ('SD_WRITE',    'SD 변경',   NOW(), 'system', NOW(), 'system'),
    ('MM_READ',     'MM 조회',   NOW(), 'system', NOW(), 'system'),
    ('MM_WRITE',    'MM 변경',   NOW(), 'system', NOW(), 'system'),
    ('FI_READ',     'FI 조회',   NOW(), 'system', NOW(), 'system'),
    ('FI_WRITE',    'FI 변경',   NOW(), 'system', NOW(), 'system'),
    ('FI_POST',     'FI 전표확정', NOW(), 'system', NOW(), 'system'),
    ('MASTER_READ', '마스터 조회', NOW(), 'system', NOW(), 'system'),
    ('MASTER_WRITE','마스터 변경', NOW(), 'system', NOW(), 'system');

-- 3) role_permission — 역할에 권한 매핑 (code 로 join)
-- SALES: SD_READ/WRITE + MASTER_READ
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'SALES' AND p.code IN ('SD_READ','SD_WRITE','MASTER_READ');
-- PURCHASING: MM_READ/WRITE + MASTER_READ
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'PURCHASING' AND p.code IN ('MM_READ','MM_WRITE','MASTER_READ');
-- FINANCE: FI_READ/WRITE/POST + MASTER_READ
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'FINANCE' AND p.code IN ('FI_READ','FI_WRITE','FI_POST','MASTER_READ');
-- ADMIN: 전 권한
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'ADMIN';

-- 4) 관리자 직원 + 코드 시퀀스 보정 (EMP 다음 번호 4 → 5)
INSERT INTO employee (code, name, email, department_id, hire_date, status,
                      created_at, created_by, updated_at, updated_by, deleted_at)
SELECT CONCAT('EMP-', YEAR(NOW()), '-0004'), '관리자', 'admin@hwlee-erp.example',
       d.id, '2024-01-01', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM department d WHERE d.code = 'DEPT-HQ';

UPDATE code_sequence SET next_number = 5 WHERE prefix = 'EMP';

-- 5) app_user — 기존+관리자 직원을 로그인 계정으로 (username = email, 비밀번호 pass1234)
INSERT INTO app_user (employee_id, username, password_hash, enabled, account_locked,
                      created_at, created_by, updated_at, updated_by)
SELECT e.id, e.email, '$2y$10$fiDYm50/gYzbfsId2MFJqOYpNXfQwoGAX79657DhyryCMTzgbPRoO',
       TRUE, FALSE, NOW(), 'system', NOW(), 'system'
  FROM employee e
 WHERE e.email IN ('kim@hwlee-erp.example', 'lee@hwlee-erp.example',
                   'park@hwlee-erp.example', 'admin@hwlee-erp.example');

-- 6) user_role — 부서 기반 자동 부여 (영업팀→SALES, 재무팀→FINANCE, 구매팀→PURCHASING)
--    부서코드 → 역할코드 매핑으로 직원의 부서를 보고 역할을 시드. 이후 관리자가 화면에서 수동 조정.
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
  FROM app_user u
  JOIN employee e   ON e.id = u.employee_id
  JOIN department d ON d.id = e.department_id
  JOIN role r ON r.code = CASE d.code
      WHEN 'DEPT-SALES'    THEN 'SALES'
      WHEN 'DEPT-PURCHASE' THEN 'PURCHASING'
      WHEN 'DEPT-FINANCE'  THEN 'FINANCE'
  END
 WHERE d.code IN ('DEPT-SALES', 'DEPT-PURCHASE', 'DEPT-FINANCE');

-- 7) 관리자 계정에 ADMIN 역할 (부서 매핑과 별개로 명시 부여)
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
  FROM app_user u
  JOIN role r ON r.code = 'ADMIN'
 WHERE u.username = 'admin@hwlee-erp.example';

-- 결과(시연용):
--   kim@hwlee-erp.example   / pass1234 → SALES   (영업팀)
--   lee@hwlee-erp.example   / pass1234 → FINANCE (재무팀)
--   park@hwlee-erp.example  / pass1234 → (역할 없음 — 생산팀, 인증되나 403 시연용)
--   admin@hwlee-erp.example / pass1234 → ADMIN
