-- 전자결재 기반: 조직에 "부서장(長)" 지정 + 본부장 3명 계정 신설.
-- 결재선은 상신자 부서에서 조직 트리를 타고 올라가며 각 노드의 부서장을 결재 단계로 삼는다.
-- (담당 → 팀장[팀 부서장] → 본부장[본부 부서장] → 대표[회사 부서장=admin])

-- 1) department 에 부서장 FK 추가 (nullable — 미지정 노드는 결재선에서 건너뜀).
ALTER TABLE department
    ADD COLUMN manager_id BIGINT NULL COMMENT '부서장(장). 전자결재 결재선의 결재자' AFTER parent_id,
    ADD CONSTRAINT fk_department_manager FOREIGN KEY (manager_id) REFERENCES employee (id);

-- 2) 본부장 3명 직원 신설 (영업/생산/경영지원 각 본부 소속, DIRECTOR 급).
--    기존 EMP-YYYY-NNNN(4자리)과 충돌 않도록 9xxx 번호대 사용.
INSERT INTO employee (code, name, email, department_id, hire_date, status, created_at, created_by, updated_at, updated_by, deleted_at)
SELECT 'EMP-2024-9001', '서동현', 'sales.dir@hyunwoo.com', d.id, '2024-01-02', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-SALESHQ'
UNION ALL
SELECT 'EMP-2024-9002', '조인성', 'prod.dir@hyunwoo.com', d.id, '2024-01-02', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODHQ'
UNION ALL
SELECT 'EMP-2024-9003', '문재현', 'mgmt.dir@hyunwoo.com', d.id, '2024-01-02', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-MGMT';

-- 3) 본부장 급여계약 (DIRECTOR, 발효 2024-01-01).
INSERT INTO employment_contract (employee_id, position, base_salary, contracted_hours, effective_from, effective_to, status, created_at, created_by, updated_at, updated_by)
SELECT e.id, 'DIRECTOR', 6000000.00, 209, '2024-01-02', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email IN ('sales.dir@hyunwoo.com', 'prod.dir@hyunwoo.com', 'mgmt.dir@hyunwoo.com');

-- 4) 본부장 로그인 계정 (비번 pass1234(BCrypt), username=email).
INSERT INTO app_user (employee_id, username, password_hash, enabled, account_locked, created_at, created_by, updated_at, updated_by)
SELECT e.id, 'sales.dir@hyunwoo.com', '$2y$10$fiDYm50/gYzbfsId2MFJqOYpNXfQwoGAX79657DhyryCMTzgbPRoO', TRUE, FALSE, NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'sales.dir@hyunwoo.com'
UNION ALL
SELECT e.id, 'prod.dir@hyunwoo.com', '$2y$10$fiDYm50/gYzbfsId2MFJqOYpNXfQwoGAX79657DhyryCMTzgbPRoO', TRUE, FALSE, NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'prod.dir@hyunwoo.com'
UNION ALL
SELECT e.id, 'mgmt.dir@hyunwoo.com', '$2y$10$fiDYm50/gYzbfsId2MFJqOYpNXfQwoGAX79657DhyryCMTzgbPRoO', TRUE, FALSE, NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'mgmt.dir@hyunwoo.com';

-- 5) 본부장 역할 매핑 (영업본부장=SALES, 생산본부장=PRODUCTION, 경영지원본부장=FINANCE).
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM app_user u JOIN role r ON r.code = 'SALES'      WHERE u.username = 'sales.dir@hyunwoo.com'
UNION ALL
SELECT u.id, r.id FROM app_user u JOIN role r ON r.code = 'PRODUCTION' WHERE u.username = 'prod.dir@hyunwoo.com'
UNION ALL
SELECT u.id, r.id FROM app_user u JOIN role r ON r.code = 'FINANCE'    WHERE u.username = 'mgmt.dir@hyunwoo.com';

-- 6) 부서장 지정 (팀 → 팀장, 본부 → 본부장, 회사 → 대표[admin]).
UPDATE department d JOIN employee e ON e.email = 'sales.mgr@hyunwoo.com'    SET d.manager_id = e.id, d.updated_at = NOW(), d.updated_by = 'system' WHERE d.code = 'DEPT-SALES';
UPDATE department d JOIN employee e ON e.email = 'finance.mgr@hyunwoo.com'  SET d.manager_id = e.id, d.updated_at = NOW(), d.updated_by = 'system' WHERE d.code = 'DEPT-FINANCE';
UPDATE department d JOIN employee e ON e.email = 'hr.mgr@hyunwoo.com'       SET d.manager_id = e.id, d.updated_at = NOW(), d.updated_by = 'system' WHERE d.code = 'DEPT-HR';
UPDATE department d JOIN employee e ON e.email = 'purchase.mgr@hyunwoo.com' SET d.manager_id = e.id, d.updated_at = NOW(), d.updated_by = 'system' WHERE d.code = 'DEPT-PURCHASE';
UPDATE department d JOIN employee e ON e.email = 'prod.sw@hyunwoo.com'      SET d.manager_id = e.id, d.updated_at = NOW(), d.updated_by = 'system' WHERE d.code = 'DEPT-PROD-SW';
UPDATE department d JOIN employee e ON e.email = 'prod.gm@hyunwoo.com'      SET d.manager_id = e.id, d.updated_at = NOW(), d.updated_by = 'system' WHERE d.code = 'DEPT-PROD-GM';
UPDATE department d JOIN employee e ON e.email = 'sales.dir@hyunwoo.com'    SET d.manager_id = e.id, d.updated_at = NOW(), d.updated_by = 'system' WHERE d.code = 'DEPT-SALESHQ';
UPDATE department d JOIN employee e ON e.email = 'prod.dir@hyunwoo.com'     SET d.manager_id = e.id, d.updated_at = NOW(), d.updated_by = 'system' WHERE d.code = 'DEPT-PRODHQ';
UPDATE department d JOIN employee e ON e.email = 'mgmt.dir@hyunwoo.com'     SET d.manager_id = e.id, d.updated_at = NOW(), d.updated_by = 'system' WHERE d.code = 'DEPT-MGMT';
UPDATE department d JOIN employee e ON e.email = 'admin@hyunwoo.com'        SET d.manager_id = e.id, d.updated_at = NOW(), d.updated_by = 'system' WHERE d.code = 'DEPT-HQ';
