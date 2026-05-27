-- Phase 3: 시연용 MM 시드.
--
-- Phase 2 시연에서 노트북 10대 수주가 만들어졌지만, 실제 재고는 아직 없다.
-- Phase 3 시연 출고 흐름을 위해 본사창고에 노트북 50대 @ 100만원 초기 입고를 시드한다.
--
-- 시드는 도메인 메서드를 부르지 못하므로 SQL 차원에서 정합성을 직접 맞춘다:
--   1) goods_receipt + goods_receipt_line (POSTED)
--   2) stock (qty_on_hand=50, average_cost=1000000)
--   3) stock_movement (+50, reason=GOODS_RECEIPT, ref=GR id)

-- 1) 본사창고 (Department 식 명시적 코드)
INSERT INTO warehouse (code, name, address, status, created_at, created_by, updated_at, updated_by, deleted_at)
VALUES ('WH-HQ', '본사창고', '서울시 강남구', 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL);

SET @wh_id = LAST_INSERT_ID();

-- 2) 초기 입고 헤더 (POSTED 로 직접 시드)
INSERT INTO goods_receipt (number, vendor_id, warehouse_id, status, receipt_date, posted_at,
                           created_at, created_by, updated_at, updated_by)
SELECT CONCAT('GR-', DATE_FORMAT(NOW(), '%Y%m%d'), '-001'),
       v.id, @wh_id, 'POSTED', CURDATE(), NOW(),
       NOW(), 'system', NOW(), 'system'
  FROM vendor v WHERE v.code = CONCAT('VEND-', YEAR(NOW()), '-0001');

SET @gr_id = LAST_INSERT_ID();

-- 3) 입고 라인 — 노트북 50대 @ 100만원
INSERT INTO goods_receipt_line (goods_receipt_id, line_no, item_id, quantity, unit_cost, line_total,
                                created_at, created_by, updated_at, updated_by)
SELECT @gr_id, 1, i.id, 50.0000, 1000000.00, 50000000.00,
       NOW(), 'system', NOW(), 'system'
  FROM item i WHERE i.code = CONCAT('ITEM-', YEAR(NOW()), '-0001');

-- 4) Stock 캐시 행 — qty=50, avg=100만, version=0
INSERT INTO stock (item_id, warehouse_id, qty_on_hand, average_cost, version,
                   created_at, created_by, updated_at, updated_by)
SELECT i.id, @wh_id, 50.0000, 1000000.00, 0,
       NOW(), 'system', NOW(), 'system'
  FROM item i WHERE i.code = CONCAT('ITEM-', YEAR(NOW()), '-0001');

-- 5) 원장에 +50 적재
INSERT INTO stock_movement (item_id, warehouse_id, qty_delta, unit_cost, reason, ref_type, ref_id, moved_at,
                            created_at, created_by, updated_at, updated_by)
SELECT i.id, @wh_id, 50.0000, 1000000.00, 'GOODS_RECEIPT', 'GR', @gr_id, NOW(),
       NOW(), 'system', NOW(), 'system'
  FROM item i WHERE i.code = CONCAT('ITEM-', YEAR(NOW()), '-0001');

-- 6) 트랜잭션 시퀀스 초기화 — 오늘 날짜로 GR 001 사용했으므로 next_number=2
INSERT INTO code_sequence (prefix, period_key, next_number, updated_at) VALUES
    ('GR', DATE_FORMAT(NOW(), '%Y%m%d'), 2, NOW());
