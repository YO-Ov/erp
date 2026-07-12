-- 임원(DIRECTOR) 역할 신설 — 손익·매출 등 전사 리포트를 임원이 볼 수 있게 한다.
--
-- 배경: 기존 역할은 부서 기능(SALES/PURCHASING/PRODUCTION/FINANCE/HR) + ADMIN 뿐이라
--       '임원'이라는 개념이 보안 역할로 없었다. 본부(HQ) 소속 = 본부장 = 임원으로 보고
--       DIRECTOR 역할을 만들어 부여한다. 리포트 열람은 ReportController 에서
--       hasAnyRole('FINANCE','DIRECTOR','ADMIN') 로 확장한다(역할 기반 enforcement).

-- 1) 역할
INSERT INTO role (code, name, created_at, created_by, updated_at, updated_by) VALUES
    ('DIRECTOR', '임원', NOW(), 'system', NOW(), 'system');

-- 2) user_role — 본부(HQ) 소속(영업본부·생산본부·경영지원본부)의 본부장에게 DIRECTOR 부여.
--    (V36 의 부서 기준 역할 부여 패턴과 동일)
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
  FROM app_user u
  JOIN employee e   ON e.id = u.employee_id
  JOIN department d ON d.id = e.department_id
  JOIN role r       ON r.code = 'DIRECTOR'
 WHERE d.code IN ('DEPT-SALESHQ', 'DEPT-PRODHQ', 'DEPT-MGMT');
