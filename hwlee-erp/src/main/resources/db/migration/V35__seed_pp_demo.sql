-- Phase 8: PP 시연 시드 — 부품 5종 + 노트북 BOM + 부품 기초재고(+개시 분개).
--
-- 완제품 'hyunwoo 노트북 15"' 를 만들기 위한 부품과 레시피를 깔고, 생산이 바로 돌도록
-- 본사창고(WH-HQ)에 부품 기초재고 500개씩을 적재한다. 재고원장과 GL 일치를 위해
-- 개시 분개(차)원재료 / 대)이익잉여금)도 함께 남긴다(실무의 '기초재고 이월').

-- 1) 부품 Item 5종 (itemType=COMPONENT, category=PART).
INSERT INTO item (code, name, category, item_type, unit, standard_cost, standard_price, status,
                  created_at, created_by, updated_at, updated_by, deleted_at)
VALUES
    (CONCAT('ITEM-', YEAR(NOW()), '-0003'), '15" LCD 패널', 'PART', 'COMPONENT', 'EA', 200000.00, 200000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    (CONCAT('ITEM-', YEAR(NOW()), '-0004'), '메인보드',     'PART', 'COMPONENT', 'EA', 150000.00, 150000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    (CONCAT('ITEM-', YEAR(NOW()), '-0005'), '16GB 메모리',  'PART', 'COMPONENT', 'EA',  50000.00,  50000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    (CONCAT('ITEM-', YEAR(NOW()), '-0006'), '512GB SSD',    'PART', 'COMPONENT', 'EA', 150000.00, 150000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    (CONCAT('ITEM-', YEAR(NOW()), '-0007'), '배터리팩',     'PART', 'COMPONENT', 'EA',  80000.00,  80000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL);

UPDATE code_sequence SET next_number = 8 WHERE prefix = 'ITEM';

-- 2) 노트북 BOM (완제품 1대당 부품 소요량). 완제품: 'hyunwoo 노트북 15"'.
INSERT INTO bom (product_item_id, component_item_id, quantity, created_at, created_by, updated_at, updated_by)
SELECT p.id, c.id, q.qty, NOW(), 'system', NOW(), 'system'
  FROM item p
  JOIN (
        SELECT '15" LCD 패널' AS cname, 1 AS qty
        UNION ALL SELECT '메인보드',    1
        UNION ALL SELECT '16GB 메모리', 2
        UNION ALL SELECT '512GB SSD',   1
        UNION ALL SELECT '배터리팩',    1
       ) q ON 1 = 1
  JOIN item c ON c.name = q.cname AND c.item_type = 'COMPONENT'
 WHERE p.name = 'hyunwoo 노트북 15"' AND p.item_type = 'FINISHED';

-- 3) 부품 기초재고 500개씩 (본사창고 WH-HQ) — 재고 캐시 + 원장.
INSERT INTO stock (item_id, warehouse_id, qty_on_hand, average_cost, version,
                   created_at, created_by, updated_at, updated_by)
SELECT c.id, w.id, 500, c.standard_cost, 0, NOW(), 'system', NOW(), 'system'
  FROM item c
  JOIN warehouse w ON w.code = 'WH-HQ'
 WHERE c.item_type = 'COMPONENT';

INSERT INTO stock_movement (item_id, warehouse_id, qty_delta, unit_cost, reason, ref_type, ref_id, moved_at,
                            created_at, created_by, updated_at, updated_by)
SELECT c.id, w.id, 500, c.standard_cost, 'ADJUSTMENT_PLUS', 'OPENING', NULL, NOW(),
       NOW(), 'system', NOW(), 'system'
  FROM item c
  JOIN warehouse w ON w.code = 'WH-HQ'
 WHERE c.item_type = 'COMPONENT';

-- 4) 개시 분개 — 차)원재료 / 대)이익잉여금. 부품 기초재고 총액 = 500 × Σ(부품 표준원가).
--    500 × (200000+150000+50000+150000+80000) = 500 × 630000 = 315,000,000
INSERT INTO journal_entry (number, entry_date, description, status, source_type, source_id, posted_at,
                           created_at, created_by, updated_at, updated_by)
VALUES ('JE-PP-OPEN-01', '2026-01-01', '생산 부품 기초재고 이월', 'POSTED', 'MANUAL', NULL, NOW(),
        NOW(), 'system', NOW(), 'system');

INSERT INTO journal_line (journal_entry_id, line_no, account_id, debit, credit,
                          created_at, created_by, updated_at, updated_by)
SELECT je.id, 1, a.id, 315000000.00, 0, NOW(), 'system', NOW(), 'system'
  FROM journal_entry je JOIN account a ON a.code = '1410'
 WHERE je.number = 'JE-PP-OPEN-01';

INSERT INTO journal_line (journal_entry_id, line_no, account_id, debit, credit,
                          created_at, created_by, updated_at, updated_by)
SELECT je.id, 2, a.id, 0, 315000000.00, NOW(), 'system', NOW(), 'system'
  FROM journal_entry je JOIN account a ON a.code = '3100'
 WHERE je.number = 'JE-PP-OPEN-01';
