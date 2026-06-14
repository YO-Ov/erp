-- Phase 8 보강: 모니터 BOM 시드 — 모니터 부품 5종 + 모니터 BOM + 부품 기초재고(+개시 분개).
--
-- 완제품 'hyunwoo 모니터 27"' 를 생산할 수 있도록 부품과 레시피를 깔고, 생산이 바로 돌도록
-- 본사창고(WH-HQ)에 부품 기초재고 500개씩을 적재한다. 노트북 시드(V35)와 동일한 패턴이되,
-- 부품 이름은 노트북 부품과 겹치지 않게 한다(BOM 이 부품명으로 JOIN 하므로 중복 시 오연결 위험).

-- 1) 모니터 부품 Item 5종 (itemType=COMPONENT, category=PART). 코드는 ITEM 시퀀스 0008 부터.
INSERT INTO item (code, name, category, item_type, unit, standard_cost, standard_price, status,
                  created_at, created_by, updated_at, updated_by, deleted_at)
VALUES
    (CONCAT('ITEM-', YEAR(NOW()), '-0008'), '27" LCD 패널', 'PART', 'COMPONENT', 'EA', 250000.00, 250000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    (CONCAT('ITEM-', YEAR(NOW()), '-0009'), '스케일러 보드', 'PART', 'COMPONENT', 'EA',  80000.00,  80000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    (CONCAT('ITEM-', YEAR(NOW()), '-0010'), '전원 보드',     'PART', 'COMPONENT', 'EA',  40000.00,  40000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    (CONCAT('ITEM-', YEAR(NOW()), '-0011'), '모니터 스탠드', 'PART', 'COMPONENT', 'EA',  30000.00,  30000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL),
    (CONCAT('ITEM-', YEAR(NOW()), '-0012'), '후면 케이스',   'PART', 'COMPONENT', 'EA',  20000.00,  20000.00, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL);

UPDATE code_sequence SET next_number = 13 WHERE prefix = 'ITEM';

-- 2) 모니터 BOM (완제품 1대당 부품 소요량). 완제품: 'hyunwoo 모니터 27"'.
INSERT INTO bom (product_item_id, component_item_id, quantity, created_at, created_by, updated_at, updated_by)
SELECT p.id, c.id, q.qty, NOW(), 'system', NOW(), 'system'
  FROM item p
  JOIN (
        SELECT '27" LCD 패널' AS cname, 1 AS qty
        UNION ALL SELECT '스케일러 보드', 1
        UNION ALL SELECT '전원 보드',     1
        UNION ALL SELECT '모니터 스탠드', 1
        UNION ALL SELECT '후면 케이스',   1
       ) q ON 1 = 1
  JOIN item c ON c.name = q.cname AND c.item_type = 'COMPONENT'
 WHERE p.name = 'hyunwoo 모니터 27"' AND p.item_type = 'FINISHED';

-- 3) 모니터 부품 기초재고 500개씩 (본사창고 WH-HQ) — 재고 캐시 + 원장.
--    노트북 부품은 V35 에서 이미 적재됐으므로, 이번에 추가한 모니터 부품만 골라 넣는다
--    (stock 의 uk_stock_item_warehouse 중복 방지).
INSERT INTO stock (item_id, warehouse_id, qty_on_hand, average_cost, version,
                   created_at, created_by, updated_at, updated_by)
SELECT c.id, w.id, 500, c.standard_cost, 0, NOW(), 'system', NOW(), 'system'
  FROM item c
  JOIN warehouse w ON w.code = 'WH-HQ'
 WHERE c.name IN ('27" LCD 패널', '스케일러 보드', '전원 보드', '모니터 스탠드', '후면 케이스');

INSERT INTO stock_movement (item_id, warehouse_id, qty_delta, unit_cost, reason, ref_type, ref_id, moved_at,
                            created_at, created_by, updated_at, updated_by)
SELECT c.id, w.id, 500, c.standard_cost, 'ADJUSTMENT_PLUS', 'OPENING', NULL, NOW(),
       NOW(), 'system', NOW(), 'system'
  FROM item c
  JOIN warehouse w ON w.code = 'WH-HQ'
 WHERE c.name IN ('27" LCD 패널', '스케일러 보드', '전원 보드', '모니터 스탠드', '후면 케이스');

-- 4) 개시 분개 — 차)원재료 / 대)이익잉여금. 모니터 부품 기초재고 총액 = 500 × Σ(부품 표준원가).
--    500 × (250000+80000+40000+30000+20000) = 500 × 420000 = 210,000,000
INSERT INTO journal_entry (number, entry_date, description, status, source_type, source_id, posted_at,
                           created_at, created_by, updated_at, updated_by)
VALUES ('JE-PP-OPEN-02', '2026-01-01', '모니터 생산 부품 기초재고 이월', 'POSTED', 'MANUAL', NULL, NOW(),
        NOW(), 'system', NOW(), 'system');

INSERT INTO journal_line (journal_entry_id, line_no, account_id, debit, credit,
                          created_at, created_by, updated_at, updated_by)
SELECT je.id, 1, a.id, 210000000.00, 0, NOW(), 'system', NOW(), 'system'
  FROM journal_entry je JOIN account a ON a.code = '1410'
 WHERE je.number = 'JE-PP-OPEN-02';

INSERT INTO journal_line (journal_entry_id, line_no, account_id, debit, credit,
                          created_at, created_by, updated_at, updated_by)
SELECT je.id, 2, a.id, 0, 210000000.00, NOW(), 'system', NOW(), 'system'
  FROM journal_entry je JOIN account a ON a.code = '3100'
 WHERE je.number = 'JE-PP-OPEN-02';
