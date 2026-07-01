-- STEP 4 (실무 리얼리즘 확장): 신규 부품 기초재고 (3개 공장창고) + 개시 분개
-- 신규 부품 24종 × 500개 × 공장창고 3곳. 차)원재료(1410)/대)이익잉여금(3100). 총액 = 4,357,500,000원.

-- 신규 부품만(기존 부품은 V35/V45 에서 WH-HQ 적재 완료). 각 공장창고에 동일 수량 적재.
-- 1) stock 캐시.
INSERT INTO stock (item_id, warehouse_id, qty_on_hand, average_cost, version, created_at, created_by, updated_at, updated_by)
SELECT c.id, w.id, 500, c.standard_cost, 0, NOW(), 'system', NOW(), 'system'
  FROM item c JOIN warehouse w ON w.code = 'WH-SW'
 WHERE c.item_type = 'COMPONENT' AND c.name IN ('LCD 패널 13"', 'LCD 패널 17"', 'LCD 패널 24"', 'LCD 패널 32"', 'LCD 패널 10"(태블릿)', 'LCD 패널 12"(태블릿)', '데스크탑 메인보드', 'CPU 모듈(보급형)', 'CPU 모듈(고성능)', 'GPU 모듈', '메모리 8GB', '메모리 32GB', 'SSD 256GB', 'SSD 1TB', '태블릿 배터리', '전원어댑터', '내부 케이블세트', '노트북 케이스', '데스크탑 케이스', 'AIO 케이스', '키캡세트', '기계식 스위치', '광센서 모듈', '마우스 하우징');

INSERT INTO stock (item_id, warehouse_id, qty_on_hand, average_cost, version, created_at, created_by, updated_at, updated_by)
SELECT c.id, w.id, 500, c.standard_cost, 0, NOW(), 'system', NOW(), 'system'
  FROM item c JOIN warehouse w ON w.code = 'WH-GM'
 WHERE c.item_type = 'COMPONENT' AND c.name IN ('LCD 패널 13"', 'LCD 패널 17"', 'LCD 패널 24"', 'LCD 패널 32"', 'LCD 패널 10"(태블릿)', 'LCD 패널 12"(태블릿)', '데스크탑 메인보드', 'CPU 모듈(보급형)', 'CPU 모듈(고성능)', 'GPU 모듈', '메모리 8GB', '메모리 32GB', 'SSD 256GB', 'SSD 1TB', '태블릿 배터리', '전원어댑터', '내부 케이블세트', '노트북 케이스', '데스크탑 케이스', 'AIO 케이스', '키캡세트', '기계식 스위치', '광센서 모듈', '마우스 하우징');

INSERT INTO stock (item_id, warehouse_id, qty_on_hand, average_cost, version, created_at, created_by, updated_at, updated_by)
SELECT c.id, w.id, 500, c.standard_cost, 0, NOW(), 'system', NOW(), 'system'
  FROM item c JOIN warehouse w ON w.code = 'WH-GJ'
 WHERE c.item_type = 'COMPONENT' AND c.name IN ('LCD 패널 13"', 'LCD 패널 17"', 'LCD 패널 24"', 'LCD 패널 32"', 'LCD 패널 10"(태블릿)', 'LCD 패널 12"(태블릿)', '데스크탑 메인보드', 'CPU 모듈(보급형)', 'CPU 모듈(고성능)', 'GPU 모듈', '메모리 8GB', '메모리 32GB', 'SSD 256GB', 'SSD 1TB', '태블릿 배터리', '전원어댑터', '내부 케이블세트', '노트북 케이스', '데스크탑 케이스', 'AIO 케이스', '키캡세트', '기계식 스위치', '광센서 모듈', '마우스 하우징');

-- 2) stock_movement 원장 (+OPENING).
INSERT INTO stock_movement (item_id, warehouse_id, qty_delta, unit_cost, reason, ref_type, ref_id, moved_at, created_at, created_by, updated_at, updated_by)
SELECT c.id, w.id, 500, c.standard_cost, 'ADJUSTMENT_PLUS', 'OPENING', NULL, NOW(), NOW(), 'system', NOW(), 'system'
  FROM item c JOIN warehouse w ON w.code = 'WH-SW'
 WHERE c.item_type = 'COMPONENT' AND c.name IN ('LCD 패널 13"', 'LCD 패널 17"', 'LCD 패널 24"', 'LCD 패널 32"', 'LCD 패널 10"(태블릿)', 'LCD 패널 12"(태블릿)', '데스크탑 메인보드', 'CPU 모듈(보급형)', 'CPU 모듈(고성능)', 'GPU 모듈', '메모리 8GB', '메모리 32GB', 'SSD 256GB', 'SSD 1TB', '태블릿 배터리', '전원어댑터', '내부 케이블세트', '노트북 케이스', '데스크탑 케이스', 'AIO 케이스', '키캡세트', '기계식 스위치', '광센서 모듈', '마우스 하우징');

INSERT INTO stock_movement (item_id, warehouse_id, qty_delta, unit_cost, reason, ref_type, ref_id, moved_at, created_at, created_by, updated_at, updated_by)
SELECT c.id, w.id, 500, c.standard_cost, 'ADJUSTMENT_PLUS', 'OPENING', NULL, NOW(), NOW(), 'system', NOW(), 'system'
  FROM item c JOIN warehouse w ON w.code = 'WH-GM'
 WHERE c.item_type = 'COMPONENT' AND c.name IN ('LCD 패널 13"', 'LCD 패널 17"', 'LCD 패널 24"', 'LCD 패널 32"', 'LCD 패널 10"(태블릿)', 'LCD 패널 12"(태블릿)', '데스크탑 메인보드', 'CPU 모듈(보급형)', 'CPU 모듈(고성능)', 'GPU 모듈', '메모리 8GB', '메모리 32GB', 'SSD 256GB', 'SSD 1TB', '태블릿 배터리', '전원어댑터', '내부 케이블세트', '노트북 케이스', '데스크탑 케이스', 'AIO 케이스', '키캡세트', '기계식 스위치', '광센서 모듈', '마우스 하우징');

INSERT INTO stock_movement (item_id, warehouse_id, qty_delta, unit_cost, reason, ref_type, ref_id, moved_at, created_at, created_by, updated_at, updated_by)
SELECT c.id, w.id, 500, c.standard_cost, 'ADJUSTMENT_PLUS', 'OPENING', NULL, NOW(), NOW(), 'system', NOW(), 'system'
  FROM item c JOIN warehouse w ON w.code = 'WH-GJ'
 WHERE c.item_type = 'COMPONENT' AND c.name IN ('LCD 패널 13"', 'LCD 패널 17"', 'LCD 패널 24"', 'LCD 패널 32"', 'LCD 패널 10"(태블릿)', 'LCD 패널 12"(태블릿)', '데스크탑 메인보드', 'CPU 모듈(보급형)', 'CPU 모듈(고성능)', 'GPU 모듈', '메모리 8GB', '메모리 32GB', 'SSD 256GB', 'SSD 1TB', '태블릿 배터리', '전원어댑터', '내부 케이블세트', '노트북 케이스', '데스크탑 케이스', 'AIO 케이스', '키캡세트', '기계식 스위치', '광센서 모듈', '마우스 하우징');

-- 3) 개시 분개 — 차)원재료(1410) / 대)이익잉여금(3100). 부품 기초재고 총액.
INSERT INTO journal_entry (number, entry_date, description, status, source_type, source_id, posted_at, created_at, created_by, updated_at, updated_by)
VALUES ('JE-STEP4-OPEN-01', '2024-01-01', 'STEP4 공장창고 부품 기초재고 이월', 'POSTED', 'MANUAL', NULL, NOW(), NOW(), 'system', NOW(), 'system');

INSERT INTO journal_line (journal_entry_id, line_no, account_id, debit, credit, created_at, created_by, updated_at, updated_by)
SELECT je.id, 1, a.id, 4357500000.00, 0, NOW(), 'system', NOW(), 'system'
  FROM journal_entry je JOIN account a ON a.code = '1410' WHERE je.number = 'JE-STEP4-OPEN-01';

INSERT INTO journal_line (journal_entry_id, line_no, account_id, debit, credit, created_at, created_by, updated_at, updated_by)
SELECT je.id, 2, a.id, 0, 4357500000.00, NOW(), 'system', NOW(), 'system'
  FROM journal_entry je JOIN account a ON a.code = '3100' WHERE je.number = 'JE-STEP4-OPEN-01';
