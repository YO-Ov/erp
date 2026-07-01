-- STEP 4 (실무 리얼리즘 확장): 거래처/공급사 확장 (기존 1 + 신규 17 ≈ 18)
-- 부품군별 공급사. 설립 초기(2024)에 공급망 구축 → VEND-2024 로 확보.

INSERT INTO vendor (code, name, business_no, address, payment_terms, status, created_at, created_by, updated_at, updated_by, deleted_at) VALUES
    ('VEND-2024-0001', '동진디스플레이', '698-39-27265', '강원 춘천시', 'NET30', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('VEND-2024-0002', '우성반도체', '835-62-24864', '부산시 해운대구', 'NET30', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('VEND-2024-0003', '한빛스토리지', '739-39-34387', '서울시 금천구', 'NET30', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('VEND-2024-0004', '대륙PCB', '439-33-31044', '광주시 광산구', 'NET30', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('VEND-2024-0005', '정밀배터리', '913-96-39522', '서울시 금천구', 'NET60', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('VEND-2024-0006', '삼우정밀', '824-22-69763', '대구시 달서구', 'NET30', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('VEND-2024-0007', '코리아파워', '300-18-85098', '울산시 남구', 'NET30', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('VEND-2024-0008', '신성입력기기', '571-79-91960', '인천시 남동구', 'NET60', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('VEND-2024-0009', '한일케이블', '464-55-24176', '서울시 강남구', 'NET60', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('VEND-2024-0010', '미래메모리', '262-23-44416', '서울시 강남구', 'NET60', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('VEND-2024-0011', '광성전자', '895-24-60654', '강원 춘천시', 'NET60', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('VEND-2024-0012', '대한소재', '870-60-69580', '대구시 달서구', 'NET60', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('VEND-2024-0013', '성진부품', '798-46-17117', '경기도 안양시', 'NET60', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('VEND-2024-0014', '동양전자부품', '649-61-95718', '충북 청주시', 'NET60', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('VEND-2024-0015', '케이텍정밀', '361-71-82916', '전북 전주시', 'NET60', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('VEND-2024-0016', '우진화학소재', '213-54-74403', '부산시 해운대구', 'NET60', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('VEND-2024-0017', '한국사출', '846-65-95057', '경기도 안양시', 'NET30', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL);

INSERT INTO code_sequence (prefix, period_key, next_number, updated_at) VALUES
    ('VEND', '2024', 18, NOW())
ON DUPLICATE KEY UPDATE next_number = VALUES(next_number), updated_at = NOW();
