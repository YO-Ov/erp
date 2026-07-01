-- STEP 4 (실무 리얼리즘 확장): 로그인 계정 8 신규 + 역할 매핑 (부서별 담당/팀장)
-- 결재라인 대비. 비밀번호 pass1234(BCrypt). username=email. user_role 명시 부여.

-- 1) 로그인 계정.
INSERT INTO app_user (employee_id, username, password_hash, enabled, account_locked, created_at, created_by, updated_at, updated_by)
SELECT emp.id, 'sales.mgr@hyunwoo.com', '$2y$10$fiDYm50/gYzbfsId2MFJqOYpNXfQwoGAX79657DhyryCMTzgbPRoO', TRUE, FALSE, NOW(), 'system', NOW(), 'system' FROM employee emp WHERE emp.email = 'sales.mgr@hyunwoo.com'
UNION ALL
SELECT emp.id, 'sales.global@hyunwoo.com', '$2y$10$fiDYm50/gYzbfsId2MFJqOYpNXfQwoGAX79657DhyryCMTzgbPRoO', TRUE, FALSE, NOW(), 'system', NOW(), 'system' FROM employee emp WHERE emp.email = 'sales.global@hyunwoo.com'
UNION ALL
SELECT emp.id, 'purchase@hyunwoo.com', '$2y$10$fiDYm50/gYzbfsId2MFJqOYpNXfQwoGAX79657DhyryCMTzgbPRoO', TRUE, FALSE, NOW(), 'system', NOW(), 'system' FROM employee emp WHERE emp.email = 'purchase@hyunwoo.com'
UNION ALL
SELECT emp.id, 'purchase.mgr@hyunwoo.com', '$2y$10$fiDYm50/gYzbfsId2MFJqOYpNXfQwoGAX79657DhyryCMTzgbPRoO', TRUE, FALSE, NOW(), 'system', NOW(), 'system' FROM employee emp WHERE emp.email = 'purchase.mgr@hyunwoo.com'
UNION ALL
SELECT emp.id, 'finance.mgr@hyunwoo.com', '$2y$10$fiDYm50/gYzbfsId2MFJqOYpNXfQwoGAX79657DhyryCMTzgbPRoO', TRUE, FALSE, NOW(), 'system', NOW(), 'system' FROM employee emp WHERE emp.email = 'finance.mgr@hyunwoo.com'
UNION ALL
SELECT emp.id, 'hr.mgr@hyunwoo.com', '$2y$10$fiDYm50/gYzbfsId2MFJqOYpNXfQwoGAX79657DhyryCMTzgbPRoO', TRUE, FALSE, NOW(), 'system', NOW(), 'system' FROM employee emp WHERE emp.email = 'hr.mgr@hyunwoo.com'
UNION ALL
SELECT emp.id, 'prod.sw@hyunwoo.com', '$2y$10$fiDYm50/gYzbfsId2MFJqOYpNXfQwoGAX79657DhyryCMTzgbPRoO', TRUE, FALSE, NOW(), 'system', NOW(), 'system' FROM employee emp WHERE emp.email = 'prod.sw@hyunwoo.com'
UNION ALL
SELECT emp.id, 'prod.gm@hyunwoo.com', '$2y$10$fiDYm50/gYzbfsId2MFJqOYpNXfQwoGAX79657DhyryCMTzgbPRoO', TRUE, FALSE, NOW(), 'system', NOW(), 'system' FROM employee emp WHERE emp.email = 'prod.gm@hyunwoo.com';

-- 2) 역할 매핑 (계정 → 역할).
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM app_user u JOIN role r ON r.code = 'SALES' WHERE u.username = 'sales.mgr@hyunwoo.com'
UNION ALL
SELECT u.id, r.id FROM app_user u JOIN role r ON r.code = 'SALES' WHERE u.username = 'sales.global@hyunwoo.com'
UNION ALL
SELECT u.id, r.id FROM app_user u JOIN role r ON r.code = 'PURCHASING' WHERE u.username = 'purchase@hyunwoo.com'
UNION ALL
SELECT u.id, r.id FROM app_user u JOIN role r ON r.code = 'PURCHASING' WHERE u.username = 'purchase.mgr@hyunwoo.com'
UNION ALL
SELECT u.id, r.id FROM app_user u JOIN role r ON r.code = 'FINANCE' WHERE u.username = 'finance.mgr@hyunwoo.com'
UNION ALL
SELECT u.id, r.id FROM app_user u JOIN role r ON r.code = 'HR' WHERE u.username = 'hr.mgr@hyunwoo.com'
UNION ALL
SELECT u.id, r.id FROM app_user u JOIN role r ON r.code = 'PRODUCTION' WHERE u.username = 'prod.sw@hyunwoo.com'
UNION ALL
SELECT u.id, r.id FROM app_user u JOIN role r ON r.code = 'PRODUCTION' WHERE u.username = 'prod.gm@hyunwoo.com';
