-- 고객 담당자(연락처) 시드 — 데모/학습용.
-- 한 고객사에 여러 담당자(구매·경리·현장 등)를 두어 1:N 구조가 화면에서 바로 보이게 한다.
-- 대표 담당자(is_primary=1)는 고객당 1명(보통 구매담당).
--
-- customer_id 는 코드로 조인해 넣는다 → 환경(RDS/운영)별 auto_increment id 차이와 무관하게 동일 결과.
-- 이미 존재하는 시드 고객(V53)에만 붙이므로 없는 코드는 자동으로 건너뛴다.

INSERT INTO customer_contact (customer_id, name, position, phone, email, is_primary, created_at, created_by, updated_at, updated_by)
SELECT c.id, v.name, v.position, v.phone, v.email, v.is_primary, NOW(), 'system', NOW(), 'system'
FROM customer c
JOIN (
    -- 서울대학교 (공공·대형) — 3명
    SELECT 'CUST-2024-0001' AS code, '김민준' AS name, '구매팀 과장'   AS position, '02-880-5011' AS phone, 'mjkim@snu.example'     AS email, 1 AS is_primary
    UNION ALL SELECT 'CUST-2024-0001', '이서연', '재무회계팀 대리', '02-880-5012', 'sylee@snu.example',   0
    UNION ALL SELECT 'CUST-2024-0001', '박도현', '시설관리 담당',   '010-2341-5013', 'dhpark@snu.example', 0

    -- 부산광역시청 — 2명
    UNION ALL SELECT 'CUST-2024-0002', '정우진', '총무과 계장',     '051-120-2201', 'wjjung@busan.example', 1
    UNION ALL SELECT 'CUST-2024-0002', '한지아', '회계과 주무관',   '051-120-2202', 'jahan@busan.example',  0

    -- 한국전력공사 — 3명
    UNION ALL SELECT 'CUST-2024-0004', '강태호', '자재구매처 차장', '061-345-3301', 'thkang@kepco.example', 1
    UNION ALL SELECT 'CUST-2024-0004', '윤서준', '전력구매팀 과장', '061-345-3302', 'sjyoon@kepco.example', 0
    UNION ALL SELECT 'CUST-2024-0004', '임하늘', '경리팀 대리',     '010-4455-3303', 'hnlim@kepco.example',  0

    -- 국민건강보험공단 — 2명
    UNION ALL SELECT 'CUST-2024-0005', '조민서', '구매운영부 과장', '033-736-4401', 'mscho@nhis.example',  1
    UNION ALL SELECT 'CUST-2024-0005', '신유나', '재정관리부 대리', '033-736-4402', 'yushin@nhis.example', 0

    -- 한국과학기술원(KAIST) — 2명
    UNION ALL SELECT 'CUST-2025-0001', '오세훈', '연구지원팀 과장', '042-350-5501', 'shoh@kaist.example',  1
    UNION ALL SELECT 'CUST-2025-0001', '배수민', '구매계약팀 대리', '042-350-5502', 'smbae@kaist.example', 0

    -- 인천광역시교육청 — 2명
    UNION ALL SELECT 'CUST-2025-0003', '문가온', '재무팀 주무관',   '032-420-6601', 'gomoon@ice.example',  1
    UNION ALL SELECT 'CUST-2025-0003', '류지완', '시설과 담당',     '032-420-6602', 'jwryu@ice.example',   0

    -- 한국도로공사 — 2명
    UNION ALL SELECT 'CUST-2026-0004', '남건우', '구매자재처 차장', '054-811-7701', 'gwnam@ex.example',    1
    UNION ALL SELECT 'CUST-2026-0004', '전예린', '회계처 대리',     '054-811-7702', 'yrjeon@ex.example',   0

    -- 우성전자 (중견 전자) — 2명
    UNION ALL SELECT 'CUST-2024-0007', '홍성민', '구매팀 부장',     '032-812-1101', 'smhong@woosung.example', 1
    UNION ALL SELECT 'CUST-2024-0007', '고은지', '경리팀 사원',     '032-812-1102', 'ejko@woosung.example',   0

    -- 우진전자 — 2명
    UNION ALL SELECT 'CUST-2025-0004', '서지훈', '구매팀 과장',     '042-931-1201', 'jhseo@woojin.example', 1
    UNION ALL SELECT 'CUST-2025-0004', '권나윤', '자재관리 담당',   '010-7788-1202', 'nykwon@woojin.example', 0

    -- 가온전자 — 2명
    UNION ALL SELECT 'CUST-2026-0005', '황도경', '구매팀 차장',     '033-641-1301', 'dkhwang@gaon.example', 1
    UNION ALL SELECT 'CUST-2026-0005', '송민재', '재무팀 대리',     '033-641-1302', 'mjsong@gaon.example',  0

    -- 이하 중소 — 대표 담당자 1명씩
    UNION ALL SELECT 'CUST-2024-0006', '유재원', '구매담당',       '042-521-2001', 'jwyu@sinwoo.example',    1
    UNION ALL SELECT 'CUST-2024-0008', '노아린', '구매·자재 담당', '042-522-2002', 'arno@onnuri.example',    1
    UNION ALL SELECT 'CUST-2024-0009', '백승호', '구매팀장',       '032-533-2003', 'shbaek@mirae.example',   1
    UNION ALL SELECT 'CUST-2024-0010', '심규현', '대표',           '031-544-2004', 'khsim@dongbang.example', 1
    UNION ALL SELECT 'CUST-2025-0005', '차예준', '구매담당',       '042-555-2005', 'yjcha@cheongwoo.example', 1
    UNION ALL SELECT 'CUST-2025-0006', '주하은', '구매팀 과장',    '043-566-2006', 'hjju@myungsung.example',  1
    UNION ALL SELECT 'CUST-2026-0006', '표시우', '구매담당',       '031-577-2007', 'siwpyo@myungsung.example', 1
    UNION ALL SELECT 'CUST-2026-0007', '민준서', '구매·경리 담당', '031-588-2008', 'jsmin@sinwoo.example',    1
) v ON v.code = c.code;
