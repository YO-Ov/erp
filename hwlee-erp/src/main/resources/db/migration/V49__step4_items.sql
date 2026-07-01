-- STEP 4 (실무 리얼리즘 확장): 품목 대량 확장 (완제품 14 + 부품 24 신규, 기존 부품 10 재분류)
-- 카테고리(V48)·공장(V47) 이후. code=ITEM-2026-NNNN, 0013 부터 이어서 발급.

-- 1) 신규 품목 (완제품=FINISHED / 부품=COMPONENT). 부품 판매가는 원가와 동일(내부품).
INSERT INTO item (code, name, category, item_type, unit, standard_cost, standard_price, status, created_at, created_by, updated_at, updated_by, deleted_at) VALUES
    ('ITEM-2026-0013', 'hyunwoo 노트북 비즈니스 13"', 'NOTEBOOK', 'FINISHED', 'EA', 800000.00, 1200000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0014', 'hyunwoo 노트북 프로 17"', 'NOTEBOOK', 'FINISHED', 'EA', 1700000.00, 2500000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0015', 'hyunwoo 모니터 24"', 'MONITOR', 'FINISHED', 'EA', 150000.00, 250000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0016', 'hyunwoo 모니터 32" 커브드', 'MONITOR', 'FINISHED', 'EA', 350000.00, 550000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0017', 'hyunwoo 데스크탑 사무용', 'DESKTOP', 'FINISHED', 'EA', 600000.00, 900000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0018', 'hyunwoo 워크스테이션', 'DESKTOP', 'FINISHED', 'EA', 1500000.00, 2200000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0019', 'hyunwoo 올인원PC 27"', 'AIO', 'FINISHED', 'EA', 900000.00, 1300000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0020', 'hyunwoo 태블릿 10"', 'TABLET', 'FINISHED', 'EA', 320000.00, 500000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0021', 'hyunwoo 태블릿 12"', 'TABLET', 'FINISHED', 'EA', 550000.00, 800000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0022', 'hyunwoo 멤브레인 키보드', 'KEYBOARD', 'FINISHED', 'EA', 15000.00, 30000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0023', 'hyunwoo 기계식 키보드', 'KEYBOARD', 'FINISHED', 'EA', 50000.00, 90000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0024', 'hyunwoo 무선 마우스', 'MOUSE', 'FINISHED', 'EA', 20000.00, 40000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0025', 'hyunwoo 게이밍 마우스', 'MOUSE', 'FINISHED', 'EA', 40000.00, 70000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0026', 'hyunwoo USB-C 도킹스테이션', 'DOCK', 'FINISHED', 'EA', 90000.00, 150000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0027', 'LCD 패널 13"', 'PART_DISPLAY', 'COMPONENT', 'EA', 180000.00, 180000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0028', 'LCD 패널 17"', 'PART_DISPLAY', 'COMPONENT', 'EA', 300000.00, 300000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0029', 'LCD 패널 24"', 'PART_DISPLAY', 'COMPONENT', 'EA', 120000.00, 120000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0030', 'LCD 패널 32"', 'PART_DISPLAY', 'COMPONENT', 'EA', 350000.00, 350000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0031', 'LCD 패널 10"(태블릿)', 'PART_DISPLAY', 'COMPONENT', 'EA', 90000.00, 90000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0032', 'LCD 패널 12"(태블릿)', 'PART_DISPLAY', 'COMPONENT', 'EA', 140000.00, 140000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0033', '데스크탑 메인보드', 'PART_BOARD', 'COMPONENT', 'EA', 130000.00, 130000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0034', 'CPU 모듈(보급형)', 'PART_COMPUTE', 'COMPONENT', 'EA', 180000.00, 180000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0035', 'CPU 모듈(고성능)', 'PART_COMPUTE', 'COMPONENT', 'EA', 350000.00, 350000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0036', 'GPU 모듈', 'PART_COMPUTE', 'COMPONENT', 'EA', 280000.00, 280000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0037', '메모리 8GB', 'PART_MEMORY', 'COMPONENT', 'EA', 30000.00, 30000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0038', '메모리 32GB', 'PART_MEMORY', 'COMPONENT', 'EA', 90000.00, 90000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0039', 'SSD 256GB', 'PART_STORAGE', 'COMPONENT', 'EA', 60000.00, 60000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0040', 'SSD 1TB', 'PART_STORAGE', 'COMPONENT', 'EA', 250000.00, 250000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0041', '태블릿 배터리', 'PART_BATTERY', 'COMPONENT', 'EA', 60000.00, 60000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0042', '전원어댑터', 'PART_POWER', 'COMPONENT', 'EA', 25000.00, 25000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0043', '내부 케이블세트', 'PART_POWER', 'COMPONENT', 'EA', 8000.00, 8000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0044', '노트북 케이스', 'PART_CHASSIS', 'COMPONENT', 'EA', 70000.00, 70000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0045', '데스크탑 케이스', 'PART_CHASSIS', 'COMPONENT', 'EA', 50000.00, 50000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0046', 'AIO 케이스', 'PART_CHASSIS', 'COMPONENT', 'EA', 110000.00, 110000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0047', '키캡세트', 'PART_INPUT', 'COMPONENT', 'EA', 6000.00, 6000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0048', '기계식 스위치', 'PART_INPUT', 'COMPONENT', 'EA', 12000.00, 12000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0049', '광센서 모듈', 'PART_INPUT', 'COMPONENT', 'EA', 9000.00, 9000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('ITEM-2026-0050', '마우스 하우징', 'PART_INPUT', 'COMPONENT', 'EA', 5000.00, 5000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL);

UPDATE code_sequence SET next_number = 51, updated_at = NOW() WHERE prefix = 'ITEM' AND period_key = '2026';

-- 2) 기존 부품 10종 재분류 (category: PART → 세분 카테고리). 이름/코드는 유지(BOM JOIN 보존).
UPDATE item SET category = 'PART_DISPLAY', updated_at = NOW(), updated_by = 'system' WHERE name = '15" LCD 패널' AND item_type = 'COMPONENT';
UPDATE item SET category = 'PART_DISPLAY', updated_at = NOW(), updated_by = 'system' WHERE name = '27" LCD 패널' AND item_type = 'COMPONENT';
UPDATE item SET category = 'PART_BOARD', updated_at = NOW(), updated_by = 'system' WHERE name = '메인보드' AND item_type = 'COMPONENT';
UPDATE item SET category = 'PART_BOARD', updated_at = NOW(), updated_by = 'system' WHERE name = '스케일러 보드' AND item_type = 'COMPONENT';
UPDATE item SET category = 'PART_BOARD', updated_at = NOW(), updated_by = 'system' WHERE name = '전원 보드' AND item_type = 'COMPONENT';
UPDATE item SET category = 'PART_MEMORY', updated_at = NOW(), updated_by = 'system' WHERE name = '16GB 메모리' AND item_type = 'COMPONENT';
UPDATE item SET category = 'PART_STORAGE', updated_at = NOW(), updated_by = 'system' WHERE name = '512GB SSD' AND item_type = 'COMPONENT';
UPDATE item SET category = 'PART_BATTERY', updated_at = NOW(), updated_by = 'system' WHERE name = '배터리팩' AND item_type = 'COMPONENT';
UPDATE item SET category = 'PART_CHASSIS', updated_at = NOW(), updated_by = 'system' WHERE name = '모니터 스탠드' AND item_type = 'COMPONENT';
UPDATE item SET category = 'PART_CHASSIS', updated_at = NOW(), updated_by = 'system' WHERE name = '후면 케이스' AND item_type = 'COMPONENT';
