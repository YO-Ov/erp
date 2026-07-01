-- STEP 4 (실무 리얼리즘 확장): 품목 카테고리 확장 (완제품 6 + 부품 세분 10)
-- 기존 3종(NOTEBOOK/MONITOR/PART) 위에 확장. PART 는 레거시로 INACTIVE 전환.

INSERT INTO item_category (code, name, sort_order, status, created_at, created_by, updated_at, updated_by, deleted_at) VALUES
    ('DESKTOP', '데스크탑', 30, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('AIO', '올인원PC', 40, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('TABLET', '태블릿', 50, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('KEYBOARD', '키보드', 60, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('MOUSE', '마우스', 70, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('DOCK', '도킹스테이션', 80, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('PART_DISPLAY', '디스플레이', 110, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('PART_BOARD', '보드', 120, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('PART_COMPUTE', '연산모듈(CPU/GPU)', 130, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('PART_MEMORY', '메모리', 140, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('PART_STORAGE', '저장장치', 150, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('PART_BATTERY', '배터리', 160, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('PART_POWER', '전원/어댑터', 170, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('PART_CHASSIS', '섀시/케이스', 180, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('PART_INPUT', '입력장치 부품', 190, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    ('PART_ETC', '기타 부품(케이블 등)', 900, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL);

-- 기존 PART 는 세분 카테고리로 대체되므로 레거시 처리(신규 등록 드롭다운에서 숨김).
UPDATE item_category SET name = '부품(레거시)', sort_order = 990, status = 'INACTIVE', updated_at = NOW(), updated_by = 'system' WHERE code = 'PART';
