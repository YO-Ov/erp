-- Phase 2: 시연용 SD 트랜잭션 시드.
--
-- 트랜잭션은 원래 시드하지 않는 것이 원칙이지만,
-- 학습/시연용 DRAFT 상태의 견적과 수주 한 건씩만 만들어 둔다.
-- (확정/출하/인보이스는 시연 흐름에서 직접 진행)

-- 1) DRAFT 견적 1건 — 신원전자 대상, 노트북 5대 + 모니터 3대
INSERT INTO quotation (number, customer_id, status, issued_date, valid_until, total_amount,
                       created_at, created_by, updated_at, updated_by)
SELECT CONCAT('Q-', DATE_FORMAT(NOW(), '%Y%m%d'), '-001'),
       c.id, 'DRAFT', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY),
       7050000.00, NOW(), 'system', NOW(), 'system'
  FROM customer c WHERE c.code = CONCAT('CUST-', YEAR(NOW()), '-0001');

SET @q_id = LAST_INSERT_ID();

INSERT INTO quotation_line (quotation_id, line_no, item_id, quantity, unit_price, line_total,
                            created_at, created_by, updated_at, updated_by)
SELECT @q_id, 1, i.id, 5.0000, 1200000.00, 6000000.00,
       NOW(), 'system', NOW(), 'system'
  FROM item i WHERE i.code = CONCAT('ITEM-', YEAR(NOW()), '-0001');

INSERT INTO quotation_line (quotation_id, line_no, item_id, quantity, unit_price, line_total,
                            created_at, created_by, updated_at, updated_by)
SELECT @q_id, 2, i.id, 3.0000, 350000.00, 1050000.00,
       NOW(), 'system', NOW(), 'system'
  FROM item i WHERE i.code = CONCAT('ITEM-', YEAR(NOW()), '-0002');

-- 2) DRAFT 수주 1건 — 같은 고객, 노트북 10대 (시연에서 6+4 부분 출하/청구에 사용)
INSERT INTO sales_order (number, customer_id, salesperson_id, quotation_id, status, order_date,
                         total_amount, created_at, created_by, updated_at, updated_by)
SELECT CONCAT('SO-', DATE_FORMAT(NOW(), '%Y%m%d'), '-001'),
       c.id,
       (SELECT id FROM employee WHERE code = CONCAT('EMP-', YEAR(NOW()), '-0001')),
       NULL, 'DRAFT', CURDATE(),
       12000000.00, NOW(), 'system', NOW(), 'system'
  FROM customer c WHERE c.code = CONCAT('CUST-', YEAR(NOW()), '-0001');

SET @so_id = LAST_INSERT_ID();

INSERT INTO sales_order_line (sales_order_id, line_no, item_id, order_qty, shipped_qty, invoiced_qty,
                              unit_price, line_total, created_at, created_by, updated_at, updated_by)
SELECT @so_id, 1, i.id, 10.0000, 0.0000, 0.0000, 1200000.00, 12000000.00,
       NOW(), 'system', NOW(), 'system'
  FROM item i WHERE i.code = CONCAT('ITEM-', YEAR(NOW()), '-0001');

-- 3) 트랜잭션 시퀀스 초기화 — 오늘 날짜로 001 까지 사용했으므로 next_number=2
INSERT INTO code_sequence (prefix, period_key, next_number, updated_at) VALUES
    ('Q',  DATE_FORMAT(NOW(), '%Y%m%d'), 2, NOW()),
    ('SO', DATE_FORMAT(NOW(), '%Y%m%d'), 2, NOW());
