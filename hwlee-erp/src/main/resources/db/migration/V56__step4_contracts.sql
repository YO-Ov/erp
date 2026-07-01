-- STEP 4 (실무 리얼리즘 확장): 급여계약 — 신규 105 + 기존 관리자(admin) = 106 (전원 발효)
-- position 별 기본급, 209h. 발효일=입사일. 기존 4명(V31)은 이미 계약 보유.

-- 1) 관리자(admin) 계약 — V31 에서 누락됐으므로 여기서 발효(전원 급여계약 목표).
INSERT INTO employment_contract (employee_id, position, base_salary, contracted_hours, effective_from, effective_to, status, created_at, created_by, updated_at, updated_by)
SELECT e.id, 'DIRECTOR', 6500000.00, 209, '2024-01-01', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system'
  FROM employee e WHERE e.email = 'admin@hyunwoo.com';

-- 2) 신규 직원 105명 계약 (발효일 = 입사일).
INSERT INTO employment_contract (employee_id, position, base_salary, contracted_hours, effective_from, effective_to, status, created_at, created_by, updated_at, updated_by)
SELECT e.id, 'MANAGER', 4800000.00, 209, '2026-01-05', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'sales.mgr@hyunwoo.com'
UNION ALL
SELECT e.id, 'SENIOR', 3600000.00, 209, '2024-04-26', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'sales.global@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 3000000.00, 209, '2026-03-18', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'purchase@hyunwoo.com'
UNION ALL
SELECT e.id, 'MANAGER', 4800000.00, 209, '2024-12-07', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'purchase.mgr@hyunwoo.com'
UNION ALL
SELECT e.id, 'MANAGER', 4800000.00, 209, '2026-02-25', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'finance.mgr@hyunwoo.com'
UNION ALL
SELECT e.id, 'MANAGER', 4800000.00, 209, '2025-05-03', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'hr.mgr@hyunwoo.com'
UNION ALL
SELECT e.id, 'MANAGER', 4800000.00, 209, '2025-01-06', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'prod.sw@hyunwoo.com'
UNION ALL
SELECT e.id, 'MANAGER', 4800000.00, 209, '2024-12-08', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'prod.gm@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 3000000.00, 209, '2025-01-20', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0003@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 3000000.00, 209, '2025-06-03', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0004@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 3000000.00, 209, '2025-09-08', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0005@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 3000000.00, 209, '2024-09-19', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0004@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 3000000.00, 209, '2025-03-08', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0006@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 3000000.00, 209, '2025-11-03', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0007@hyunwoo.com'
UNION ALL
SELECT e.id, 'SENIOR', 3600000.00, 209, '2024-01-24', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0005@hyunwoo.com'
UNION ALL
SELECT e.id, 'SENIOR', 3600000.00, 209, '2026-01-17', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2026-0009@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-05-14', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0006@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-10-19', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0008@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2026-01-10', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2026-0010@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-03-11', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0007@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-02-13', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0009@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-10-17', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0010@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-02-06', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0011@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-05-24', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0008@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2026-05-22', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2026-0011@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-12-26', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0009@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-06-21', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0012@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-06-24', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0010@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2026-06-23', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2026-0012@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-09-15', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0011@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-04-17', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0012@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-12-20', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0013@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-12-25', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0013@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-02-03', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0014@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-04-15', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0014@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-01-06', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0015@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-12-11', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0016@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-06-21', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0015@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-12-15', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0016@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2026-02-28', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2026-0013@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-12-02', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0017@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2026-03-19', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2026-0014@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-08-01', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0018@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-02-27', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0017@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-02-28', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0018@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2026-05-20', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2026-0015@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-10-24', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0019@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-02-13', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0020@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-04-19', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0019@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-11-13', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0020@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2026-06-01', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2026-0016@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-06-23', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0021@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-02-25', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0022@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-01-18', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0023@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-10-10', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0024@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-04-06', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0025@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-07-02', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0026@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-03-26', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0027@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-01-16', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0021@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2026-02-28', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2026-0017@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-01-11', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0028@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-07-20', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0029@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-02-14', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0030@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-06-28', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0031@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2026-03-22', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2026-0018@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2026-06-25', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2026-0019@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-04-05', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0022@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-05-06', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0032@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2026-03-28', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2026-0020@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-07-17', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0033@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-03-05', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0023@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-03-08', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0034@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-06-02', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0035@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-06-21', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0024@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-10-27', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0036@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-08-03', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0025@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-01-02', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0026@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-04-17', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0037@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-04-08', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0038@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-01-11', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0027@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-06-08', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0028@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-12-28', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0039@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-08-26', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0040@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-10-16', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0041@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2026-06-23', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2026-0021@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-09-19', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0042@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-03-24', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0043@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-05-10', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0029@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2026-03-06', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2026-0022@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2026-01-20', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2026-0023@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-12-06', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0030@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-10-24', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0044@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-06-21', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0031@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-04-07', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0032@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2026-02-21', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2026-0024@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-06-26', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0045@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-01-15', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0033@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-04-03', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0046@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-05-15', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0047@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-04-24', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0048@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-06-01', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0049@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-12-16', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0050@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-07-13', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0051@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2025-08-26', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2025-0034@hyunwoo.com'
UNION ALL
SELECT e.id, 'STAFF', 2900000.00, 209, '2024-11-19', NULL, 'ACTIVE', NOW(), 'system', NOW(), 'system' FROM employee e WHERE e.email = 'emp-2024-0052@hyunwoo.com';
