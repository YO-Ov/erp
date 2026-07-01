-- STEP 4 (실무 리얼리즘 확장): 직원 마스터 대량 확장 (기존 5 + 신규 105 = 110)
-- 사무 18(로그인 일부)·생산직 92. 입사연도 2024~2026 분산 → EMP-YYYY-NNNN.

-- 직원 삽입 (부서코드로 department 조인). 로그인 계정은 V57 에서 별도 연결.
INSERT INTO employee (code, name, email, department_id, hire_date, status, created_at, created_by, updated_at, updated_by, deleted_at)
SELECT 'EMP-2026-0006', '안나윤', 'sales.mgr@hyunwoo.com', d.id, '2026-01-05', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-SALES'
UNION ALL
SELECT 'EMP-2024-0001', '윤상현', 'sales.global@hyunwoo.com', d.id, '2024-04-26', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-SALESGL'
UNION ALL
SELECT 'EMP-2026-0007', '박상현', 'purchase@hyunwoo.com', d.id, '2026-03-18', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PURCHASE'
UNION ALL
SELECT 'EMP-2024-0002', '황현우', 'purchase.mgr@hyunwoo.com', d.id, '2024-12-07', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PURCHASE'
UNION ALL
SELECT 'EMP-2026-0008', '우태윤', 'finance.mgr@hyunwoo.com', d.id, '2026-02-25', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-FINANCE'
UNION ALL
SELECT 'EMP-2025-0001', '노현우', 'hr.mgr@hyunwoo.com', d.id, '2025-05-03', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-HR'
UNION ALL
SELECT 'EMP-2025-0002', '허민재', 'prod.sw@hyunwoo.com', d.id, '2025-01-06', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2024-0003', '정성민', 'prod.gm@hyunwoo.com', d.id, '2024-12-08', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2025-0003', '임지훈', 'emp-2025-0003@hyunwoo.com', d.id, '2025-01-20', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-GA'
UNION ALL
SELECT 'EMP-2025-0004', '한민준', 'emp-2025-0004@hyunwoo.com', d.id, '2025-06-03', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-GA'
UNION ALL
SELECT 'EMP-2025-0005', '정예린', 'emp-2025-0005@hyunwoo.com', d.id, '2025-09-08', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-SALES2'
UNION ALL
SELECT 'EMP-2024-0004', '오진우', 'emp-2024-0004@hyunwoo.com', d.id, '2024-09-19', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-SALES2'
UNION ALL
SELECT 'EMP-2025-0006', '남승현', 'emp-2025-0006@hyunwoo.com', d.id, '2025-03-08', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-LOGIS'
UNION ALL
SELECT 'EMP-2025-0007', '류민서', 'emp-2025-0007@hyunwoo.com', d.id, '2025-11-03', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-LOGIS'
UNION ALL
SELECT 'EMP-2024-0005', '노지호', 'emp-2024-0005@hyunwoo.com', d.id, '2024-01-24', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-RND'
UNION ALL
SELECT 'EMP-2026-0009', '권성민', 'emp-2026-0009@hyunwoo.com', d.id, '2026-01-17', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-RND'
UNION ALL
SELECT 'EMP-2024-0006', '남규현', 'emp-2024-0006@hyunwoo.com', d.id, '2024-05-14', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2025-0008', '임지호', 'emp-2025-0008@hyunwoo.com', d.id, '2025-10-19', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2026-0010', '김지민', 'emp-2026-0010@hyunwoo.com', d.id, '2026-01-10', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2024-0007', '하소율', 'emp-2024-0007@hyunwoo.com', d.id, '2024-03-11', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2025-0009', '신예은', 'emp-2025-0009@hyunwoo.com', d.id, '2025-02-13', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2025-0010', '서혜원', 'emp-2025-0010@hyunwoo.com', d.id, '2025-10-17', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2025-0011', '차하린', 'emp-2025-0011@hyunwoo.com', d.id, '2025-02-06', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2024-0008', '신서윤', 'emp-2024-0008@hyunwoo.com', d.id, '2024-05-24', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2026-0011', '홍수아', 'emp-2026-0011@hyunwoo.com', d.id, '2026-05-22', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2024-0009', '백지훈', 'emp-2024-0009@hyunwoo.com', d.id, '2024-12-26', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2025-0012', '주서윤', 'emp-2025-0012@hyunwoo.com', d.id, '2025-06-21', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2024-0010', '장가은', 'emp-2024-0010@hyunwoo.com', d.id, '2024-06-24', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2026-0012', '남도윤', 'emp-2026-0012@hyunwoo.com', d.id, '2026-06-23', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2024-0011', '구정우', 'emp-2024-0011@hyunwoo.com', d.id, '2024-09-15', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2024-0012', '최성민', 'emp-2024-0012@hyunwoo.com', d.id, '2024-04-17', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2025-0013', '서건우', 'emp-2025-0013@hyunwoo.com', d.id, '2025-12-20', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2024-0013', '구형준', 'emp-2024-0013@hyunwoo.com', d.id, '2024-12-25', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2025-0014', '송찬민', 'emp-2025-0014@hyunwoo.com', d.id, '2025-02-03', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2024-0014', '이종현', 'emp-2024-0014@hyunwoo.com', d.id, '2024-04-15', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2024-0015', '한예린', 'emp-2024-0015@hyunwoo.com', d.id, '2024-01-06', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2024-0016', '박소율', 'emp-2024-0016@hyunwoo.com', d.id, '2024-12-11', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2025-0015', '노서연', 'emp-2025-0015@hyunwoo.com', d.id, '2025-06-21', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2025-0016', '권하은', 'emp-2025-0016@hyunwoo.com', d.id, '2025-12-15', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2026-0013', '조규현', 'emp-2026-0013@hyunwoo.com', d.id, '2026-02-28', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2024-0017', '배하은', 'emp-2024-0017@hyunwoo.com', d.id, '2024-12-02', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2026-0014', '남진우', 'emp-2026-0014@hyunwoo.com', d.id, '2026-03-19', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2024-0018', '심나윤', 'emp-2024-0018@hyunwoo.com', d.id, '2024-08-01', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-SW'
UNION ALL
SELECT 'EMP-2025-0017', '허지아', 'emp-2025-0017@hyunwoo.com', d.id, '2025-02-27', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2025-0018', '황우진', 'emp-2025-0018@hyunwoo.com', d.id, '2025-02-28', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2026-0015', '박혜원', 'emp-2026-0015@hyunwoo.com', d.id, '2026-05-20', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2025-0019', '주예은', 'emp-2025-0019@hyunwoo.com', d.id, '2025-10-24', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2025-0020', '한지훈', 'emp-2025-0020@hyunwoo.com', d.id, '2025-02-13', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2024-0019', '윤민재', 'emp-2024-0019@hyunwoo.com', d.id, '2024-04-19', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2024-0020', '손다은', 'emp-2024-0020@hyunwoo.com', d.id, '2024-11-13', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2026-0016', '윤서현', 'emp-2026-0016@hyunwoo.com', d.id, '2026-06-01', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2024-0021', '홍지민', 'emp-2024-0021@hyunwoo.com', d.id, '2024-06-23', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2024-0022', '김건우', 'emp-2024-0022@hyunwoo.com', d.id, '2024-02-25', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2024-0023', '차민재', 'emp-2024-0023@hyunwoo.com', d.id, '2024-01-18', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2024-0024', '전규현', 'emp-2024-0024@hyunwoo.com', d.id, '2024-10-10', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2024-0025', '정성호', 'emp-2024-0025@hyunwoo.com', d.id, '2024-04-06', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2024-0026', '오지훈', 'emp-2024-0026@hyunwoo.com', d.id, '2024-07-02', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2024-0027', '강예은', 'emp-2024-0027@hyunwoo.com', d.id, '2024-03-26', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2025-0021', '김소율', 'emp-2025-0021@hyunwoo.com', d.id, '2025-01-16', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2026-0017', '노지아', 'emp-2026-0017@hyunwoo.com', d.id, '2026-02-28', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2024-0028', '김지원', 'emp-2024-0028@hyunwoo.com', d.id, '2024-01-11', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2024-0029', '권윤서', 'emp-2024-0029@hyunwoo.com', d.id, '2024-07-20', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2024-0030', '신채원', 'emp-2024-0030@hyunwoo.com', d.id, '2024-02-14', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2024-0031', '노재윤', 'emp-2024-0031@hyunwoo.com', d.id, '2024-06-28', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2026-0018', '양예은', 'emp-2026-0018@hyunwoo.com', d.id, '2026-03-22', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2026-0019', '고지훈', 'emp-2026-0019@hyunwoo.com', d.id, '2026-06-25', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2025-0022', '강수아', 'emp-2025-0022@hyunwoo.com', d.id, '2025-04-05', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2024-0032', '류서연', 'emp-2024-0032@hyunwoo.com', d.id, '2024-05-06', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2026-0020', '임은지', 'emp-2026-0020@hyunwoo.com', d.id, '2026-03-28', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2024-0033', '안하린', 'emp-2024-0033@hyunwoo.com', d.id, '2024-07-17', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PROD-GM'
UNION ALL
SELECT 'EMP-2025-0023', '전준서', 'emp-2025-0023@hyunwoo.com', d.id, '2025-03-05', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2024-0034', '장형준', 'emp-2024-0034@hyunwoo.com', d.id, '2024-03-08', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2024-0035', '윤예준', 'emp-2024-0035@hyunwoo.com', d.id, '2024-06-02', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2025-0024', '우상현', 'emp-2025-0024@hyunwoo.com', d.id, '2025-06-21', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2024-0036', '곽지우', 'emp-2024-0036@hyunwoo.com', d.id, '2024-10-27', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2025-0025', '민동현', 'emp-2025-0025@hyunwoo.com', d.id, '2025-08-03', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2025-0026', '홍은지', 'emp-2025-0026@hyunwoo.com', d.id, '2025-01-02', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2024-0037', '임재현', 'emp-2024-0037@hyunwoo.com', d.id, '2024-04-17', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2024-0038', '양재현', 'emp-2024-0038@hyunwoo.com', d.id, '2024-04-08', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2025-0027', '노예은', 'emp-2025-0027@hyunwoo.com', d.id, '2025-01-11', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2025-0028', '김민재', 'emp-2025-0028@hyunwoo.com', d.id, '2025-06-08', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2024-0039', '구재현', 'emp-2024-0039@hyunwoo.com', d.id, '2024-12-28', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2024-0040', '주건우', 'emp-2024-0040@hyunwoo.com', d.id, '2024-08-26', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2024-0041', '박정우', 'emp-2024-0041@hyunwoo.com', d.id, '2024-10-16', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2026-0021', '유하린', 'emp-2026-0021@hyunwoo.com', d.id, '2026-06-23', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2024-0042', '전하린', 'emp-2024-0042@hyunwoo.com', d.id, '2024-09-19', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2024-0043', '노은지', 'emp-2024-0043@hyunwoo.com', d.id, '2024-03-24', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2025-0029', '장지우', 'emp-2025-0029@hyunwoo.com', d.id, '2025-05-10', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2026-0022', '배동현', 'emp-2026-0022@hyunwoo.com', d.id, '2026-03-06', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2026-0023', '구지연', 'emp-2026-0023@hyunwoo.com', d.id, '2026-01-20', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2025-0030', '박나윤', 'emp-2025-0030@hyunwoo.com', d.id, '2025-12-06', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2024-0044', '하민준', 'emp-2024-0044@hyunwoo.com', d.id, '2024-10-24', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2025-0031', '양수아', 'emp-2025-0031@hyunwoo.com', d.id, '2025-06-21', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2025-0032', '곽서현', 'emp-2025-0032@hyunwoo.com', d.id, '2025-04-07', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2026-0024', '임채원', 'emp-2026-0024@hyunwoo.com', d.id, '2026-02-21', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-PRODUCTION'
UNION ALL
SELECT 'EMP-2024-0045', '류성민', 'emp-2024-0045@hyunwoo.com', d.id, '2024-06-26', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-QC'
UNION ALL
SELECT 'EMP-2025-0033', '서예준', 'emp-2025-0033@hyunwoo.com', d.id, '2025-01-15', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-QC'
UNION ALL
SELECT 'EMP-2024-0046', '오지민', 'emp-2024-0046@hyunwoo.com', d.id, '2024-04-03', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-QC'
UNION ALL
SELECT 'EMP-2024-0047', '성지민', 'emp-2024-0047@hyunwoo.com', d.id, '2024-05-15', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-QC'
UNION ALL
SELECT 'EMP-2024-0048', '노유진', 'emp-2024-0048@hyunwoo.com', d.id, '2024-04-24', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-QC'
UNION ALL
SELECT 'EMP-2024-0049', '전도윤', 'emp-2024-0049@hyunwoo.com', d.id, '2024-06-01', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-QC'
UNION ALL
SELECT 'EMP-2024-0050', '문준서', 'emp-2024-0050@hyunwoo.com', d.id, '2024-12-16', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-QC'
UNION ALL
SELECT 'EMP-2024-0051', '민예준', 'emp-2024-0051@hyunwoo.com', d.id, '2024-07-13', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-QC'
UNION ALL
SELECT 'EMP-2025-0034', '구서현', 'emp-2025-0034@hyunwoo.com', d.id, '2025-08-26', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-QC'
UNION ALL
SELECT 'EMP-2024-0052', '허규현', 'emp-2024-0052@hyunwoo.com', d.id, '2024-11-19', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL FROM department d WHERE d.code = 'DEPT-QC';

-- 채번 시퀀스: 입사연도별 마지막+1.
INSERT INTO code_sequence (prefix, period_key, next_number, updated_at) VALUES
    ('EMP', '2024', 53, NOW()),
    ('EMP', '2025', 35, NOW()),
    ('EMP', '2026', 25, NOW())
ON DUPLICATE KEY UPDATE next_number = VALUES(next_number), updated_at = NOW();
