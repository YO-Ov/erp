-- Phase 7: HR 인건비 분개용 계정 5개 + HR 역할/권한.
--
-- 계정(V23 트리에 말단 추가):
--   5200 급여비용     (EXPENSE)  — 직원 총지급액 gross
--   5300 법정복리비   (EXPENSE)  — 4대보험 회사부담분
--   2400 예수금-소득세   (LIABILITY) — 떼어둔 소득세
--   2500 예수금-사회보험 (LIABILITY) — 4대보험 직원분+회사분
--   2600 미지급급여   (LIABILITY) — 직원에게 줄 실수령액

INSERT INTO account (code, name, type, parent_id, postable, status,
                     created_at, created_by, updated_at, updated_by)
SELECT '5200', '급여비용',   'EXPENSE', id, 1, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM account WHERE code = '5000';
INSERT INTO account (code, name, type, parent_id, postable, status,
                     created_at, created_by, updated_at, updated_by)
SELECT '5300', '법정복리비', 'EXPENSE', id, 1, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM account WHERE code = '5000';

INSERT INTO account (code, name, type, parent_id, postable, status,
                     created_at, created_by, updated_at, updated_by)
SELECT '2400', '예수금-소득세',   'LIABILITY', id, 1, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM account WHERE code = '2000';
INSERT INTO account (code, name, type, parent_id, postable, status,
                     created_at, created_by, updated_at, updated_by)
SELECT '2500', '예수금-사회보험', 'LIABILITY', id, 1, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM account WHERE code = '2000';
INSERT INTO account (code, name, type, parent_id, postable, status,
                     created_at, created_by, updated_at, updated_by)
SELECT '2600', '미지급급여',     'LIABILITY', id, 1, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM account WHERE code = '2000';

-- HR 역할 + 권한 (V28 패턴 — 역할 단위 인가, permission 은 구조 시드)
INSERT INTO role (code, name, created_at, created_by, updated_at, updated_by) VALUES
    ('HR', '인사', NOW(), 'system', NOW(), 'system');

INSERT INTO permission (code, name, created_at, created_by, updated_at, updated_by) VALUES
    ('HR_READ',  'HR 조회', NOW(), 'system', NOW(), 'system'),
    ('HR_WRITE', 'HR 변경', NOW(), 'system', NOW(), 'system');

-- HR 역할: HR_READ/WRITE + MASTER_READ
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'HR' AND p.code IN ('HR_READ', 'HR_WRITE', 'MASTER_READ');

-- ADMIN 역할에도 신규 HR 권한 부여 (V28 의 'ADMIN 전 권한' 은 그 시점 권한만 매핑했으므로 추가)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'ADMIN' AND p.code IN ('HR_READ', 'HR_WRITE');
