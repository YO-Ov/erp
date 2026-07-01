-- STEP 4 (실무 리얼리즘 확장): 조직(부서) 재편 + 확장 (본부-팀 2계층)
-- 기존 flat 6부서를 본부-팀 트리로 재편. 기존 코드는 유지(직원 FK·user_role 매핑 보존).

-- 1) 본부/HQ직속 팀 (parent = DEPT-HQ).
INSERT INTO department (code, name, parent_id, status, created_at, created_by, updated_at, updated_by, deleted_at)
SELECT 'DEPT-MGMT', '경영지원본부', h.id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM department h WHERE h.code = 'DEPT-HQ';
INSERT INTO department (code, name, parent_id, status, created_at, created_by, updated_at, updated_by, deleted_at)
SELECT 'DEPT-SALESHQ', '영업본부', h.id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM department h WHERE h.code = 'DEPT-HQ';
INSERT INTO department (code, name, parent_id, status, created_at, created_by, updated_at, updated_by, deleted_at)
SELECT 'DEPT-PRODHQ', '생산본부', h.id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM department h WHERE h.code = 'DEPT-HQ';
INSERT INTO department (code, name, parent_id, status, created_at, created_by, updated_at, updated_by, deleted_at)
SELECT 'DEPT-LOGIS', '물류팀', h.id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM department h WHERE h.code = 'DEPT-HQ';
INSERT INTO department (code, name, parent_id, status, created_at, created_by, updated_at, updated_by, deleted_at)
SELECT 'DEPT-RND', '연구개발팀', h.id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM department h WHERE h.code = 'DEPT-HQ';

-- 2) 본부 하위 팀 (parent = 각 본부).
INSERT INTO department (code, name, parent_id, status, created_at, created_by, updated_at, updated_by, deleted_at)
SELECT 'DEPT-GA', '총무팀', p.id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM department p WHERE p.code = 'DEPT-MGMT';
INSERT INTO department (code, name, parent_id, status, created_at, created_by, updated_at, updated_by, deleted_at)
SELECT 'DEPT-SALES2', '국내영업2팀', p.id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM department p WHERE p.code = 'DEPT-SALESHQ';
INSERT INTO department (code, name, parent_id, status, created_at, created_by, updated_at, updated_by, deleted_at)
SELECT 'DEPT-SALESGL', '해외영업팀', p.id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM department p WHERE p.code = 'DEPT-SALESHQ';
INSERT INTO department (code, name, parent_id, status, created_at, created_by, updated_at, updated_by, deleted_at)
SELECT 'DEPT-PROD-SW', '수원생산팀', p.id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM department p WHERE p.code = 'DEPT-PRODHQ';
INSERT INTO department (code, name, parent_id, status, created_at, created_by, updated_at, updated_by, deleted_at)
SELECT 'DEPT-PROD-GM', '구미생산팀', p.id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM department p WHERE p.code = 'DEPT-PRODHQ';
INSERT INTO department (code, name, parent_id, status, created_at, created_by, updated_at, updated_by, deleted_at)
SELECT 'DEPT-QC', '품질관리팀', p.id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM department p WHERE p.code = 'DEPT-PRODHQ';

-- 3) 기존 부서 재편 (이름·소속 변경. 코드 불변 → 기존 직원/권한 보존).
UPDATE department SET name = '국내영업1팀', parent_id = (SELECT id FROM (SELECT id FROM department WHERE code = 'DEPT-SALESHQ') t), updated_at = NOW(), updated_by = 'system' WHERE code = 'DEPT-SALES';
UPDATE department SET name = '광주생산팀', parent_id = (SELECT id FROM (SELECT id FROM department WHERE code = 'DEPT-PRODHQ') t), updated_at = NOW(), updated_by = 'system' WHERE code = 'DEPT-PRODUCTION';
UPDATE department SET name = '재무팀', parent_id = (SELECT id FROM (SELECT id FROM department WHERE code = 'DEPT-MGMT') t), updated_at = NOW(), updated_by = 'system' WHERE code = 'DEPT-FINANCE';
UPDATE department SET name = '인사팀', parent_id = (SELECT id FROM (SELECT id FROM department WHERE code = 'DEPT-MGMT') t), updated_at = NOW(), updated_by = 'system' WHERE code = 'DEPT-HR';
