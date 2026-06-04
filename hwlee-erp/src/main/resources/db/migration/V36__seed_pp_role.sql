-- Phase 8: PP 인가 시드 — PRODUCTION 역할 + 생산팀 매핑(박생산 복선 회수).
--
-- Phase 6 에서 박생산(park@hyunwoo.com, 생산팀)은 일부러 역할을 안 줬다(403 시연용).
-- Phase 8 에서 PRODUCTION 역할을 만들어 부여하면 그 복선이 회수된다.

-- 1) 역할
INSERT INTO role (code, name, created_at, created_by, updated_at, updated_by) VALUES
    ('PRODUCTION', '생산', NOW(), 'system', NOW(), 'system');

-- 2) 권한 (역할 단위 인가 — 구조+시드만, 실제 enforcement 는 hasAnyRole)
INSERT INTO permission (code, name, created_at, created_by, updated_at, updated_by) VALUES
    ('PP_READ',  'PP 조회', NOW(), 'system', NOW(), 'system'),
    ('PP_WRITE', 'PP 변경', NOW(), 'system', NOW(), 'system');

-- 3) role_permission — PRODUCTION: PP_READ/WRITE + MASTER_READ. ADMIN: 신규 PP 권한도 부여.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'PRODUCTION' AND p.code IN ('PP_READ', 'PP_WRITE', 'MASTER_READ');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'ADMIN' AND p.code IN ('PP_READ', 'PP_WRITE');

-- 4) user_role — 생산팀(DEPT-PRODUCTION) → PRODUCTION (박생산이 드디어 역할을 가진다).
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
  FROM app_user u
  JOIN employee e   ON e.id = u.employee_id
  JOIN department d ON d.id = e.department_id
  JOIN role r       ON r.code = 'PRODUCTION'
 WHERE d.code = 'DEPT-PRODUCTION';
