-- STEP 5: 종료 시점 재고 확정 (WH-HQ)

-- 마스터 id 세션변수 로드 (이 파일 내에서 사용)
SET @wh := (SELECT id FROM warehouse WHERE code='WH-HQ');
SET @a_1100 := (SELECT id FROM account WHERE code='1100');
SET @a_1200 := (SELECT id FROM account WHERE code='1200');
SET @a_1400 := (SELECT id FROM account WHERE code='1400');
SET @a_1410 := (SELECT id FROM account WHERE code='1410');
SET @a_2100 := (SELECT id FROM account WHERE code='2100');
SET @a_2200 := (SELECT id FROM account WHERE code='2200');
SET @a_4100 := (SELECT id FROM account WHERE code='4100');
SET @a_5100 := (SELECT id FROM account WHERE code='5100');
SET @i_2026_0001 := (SELECT id FROM item WHERE code='ITEM-2026-0001');
SET @i_2026_0002 := (SELECT id FROM item WHERE code='ITEM-2026-0002');
SET @i_2026_0013 := (SELECT id FROM item WHERE code='ITEM-2026-0013');
SET @i_2026_0014 := (SELECT id FROM item WHERE code='ITEM-2026-0014');
SET @i_2026_0015 := (SELECT id FROM item WHERE code='ITEM-2026-0015');
SET @i_2026_0016 := (SELECT id FROM item WHERE code='ITEM-2026-0016');
SET @i_2026_0017 := (SELECT id FROM item WHERE code='ITEM-2026-0017');
SET @i_2026_0018 := (SELECT id FROM item WHERE code='ITEM-2026-0018');
SET @i_2026_0019 := (SELECT id FROM item WHERE code='ITEM-2026-0019');
SET @i_2026_0020 := (SELECT id FROM item WHERE code='ITEM-2026-0020');
SET @i_2026_0021 := (SELECT id FROM item WHERE code='ITEM-2026-0021');
SET @i_2026_0022 := (SELECT id FROM item WHERE code='ITEM-2026-0022');
SET @i_2026_0023 := (SELECT id FROM item WHERE code='ITEM-2026-0023');
SET @i_2026_0024 := (SELECT id FROM item WHERE code='ITEM-2026-0024');
SET @i_2026_0025 := (SELECT id FROM item WHERE code='ITEM-2026-0025');
SET @i_2026_0026 := (SELECT id FROM item WHERE code='ITEM-2026-0026');
SET @i_2026_0003 := (SELECT id FROM item WHERE code='ITEM-2026-0003');
SET @i_2026_0004 := (SELECT id FROM item WHERE code='ITEM-2026-0004');
SET @i_2026_0005 := (SELECT id FROM item WHERE code='ITEM-2026-0005');
SET @i_2026_0006 := (SELECT id FROM item WHERE code='ITEM-2026-0006');
SET @i_2026_0007 := (SELECT id FROM item WHERE code='ITEM-2026-0007');
SET @i_2026_0008 := (SELECT id FROM item WHERE code='ITEM-2026-0008');
SET @i_2026_0009 := (SELECT id FROM item WHERE code='ITEM-2026-0009');
SET @i_2026_0010 := (SELECT id FROM item WHERE code='ITEM-2026-0010');
SET @i_2026_0011 := (SELECT id FROM item WHERE code='ITEM-2026-0011');
SET @i_2026_0012 := (SELECT id FROM item WHERE code='ITEM-2026-0012');
SET @i_2026_0027 := (SELECT id FROM item WHERE code='ITEM-2026-0027');
SET @i_2026_0028 := (SELECT id FROM item WHERE code='ITEM-2026-0028');
SET @i_2026_0029 := (SELECT id FROM item WHERE code='ITEM-2026-0029');
SET @i_2026_0030 := (SELECT id FROM item WHERE code='ITEM-2026-0030');
SET @i_2026_0031 := (SELECT id FROM item WHERE code='ITEM-2026-0031');
SET @i_2026_0032 := (SELECT id FROM item WHERE code='ITEM-2026-0032');
SET @i_2026_0033 := (SELECT id FROM item WHERE code='ITEM-2026-0033');
SET @i_2026_0034 := (SELECT id FROM item WHERE code='ITEM-2026-0034');
SET @i_2026_0035 := (SELECT id FROM item WHERE code='ITEM-2026-0035');
SET @i_2026_0036 := (SELECT id FROM item WHERE code='ITEM-2026-0036');
SET @i_2026_0037 := (SELECT id FROM item WHERE code='ITEM-2026-0037');
SET @i_2026_0038 := (SELECT id FROM item WHERE code='ITEM-2026-0038');
SET @i_2026_0039 := (SELECT id FROM item WHERE code='ITEM-2026-0039');
SET @i_2026_0040 := (SELECT id FROM item WHERE code='ITEM-2026-0040');
SET @i_2026_0041 := (SELECT id FROM item WHERE code='ITEM-2026-0041');
SET @i_2026_0042 := (SELECT id FROM item WHERE code='ITEM-2026-0042');
SET @i_2026_0043 := (SELECT id FROM item WHERE code='ITEM-2026-0043');
SET @i_2026_0044 := (SELECT id FROM item WHERE code='ITEM-2026-0044');
SET @i_2026_0045 := (SELECT id FROM item WHERE code='ITEM-2026-0045');
SET @i_2026_0046 := (SELECT id FROM item WHERE code='ITEM-2026-0046');
SET @i_2026_0047 := (SELECT id FROM item WHERE code='ITEM-2026-0047');
SET @i_2026_0048 := (SELECT id FROM item WHERE code='ITEM-2026-0048');
SET @i_2026_0049 := (SELECT id FROM item WHERE code='ITEM-2026-0049');
SET @i_2026_0050 := (SELECT id FROM item WHERE code='ITEM-2026-0050');
SET @c_2026_0001 := (SELECT id FROM customer WHERE code='CUST-2026-0001');
SET @c_2026_0002 := (SELECT id FROM customer WHERE code='CUST-2026-0002');
SET @c_2024_0001 := (SELECT id FROM customer WHERE code='CUST-2024-0001');
SET @c_2024_0002 := (SELECT id FROM customer WHERE code='CUST-2024-0002');
SET @c_2024_0003 := (SELECT id FROM customer WHERE code='CUST-2024-0003');
SET @c_2024_0004 := (SELECT id FROM customer WHERE code='CUST-2024-0004');
SET @c_2024_0005 := (SELECT id FROM customer WHERE code='CUST-2024-0005');
SET @c_2024_0006 := (SELECT id FROM customer WHERE code='CUST-2024-0006');
SET @c_2024_0007 := (SELECT id FROM customer WHERE code='CUST-2024-0007');
SET @c_2024_0008 := (SELECT id FROM customer WHERE code='CUST-2024-0008');
SET @c_2024_0009 := (SELECT id FROM customer WHERE code='CUST-2024-0009');
SET @c_2024_0010 := (SELECT id FROM customer WHERE code='CUST-2024-0010');
SET @c_2024_0011 := (SELECT id FROM customer WHERE code='CUST-2024-0011');
SET @c_2024_0012 := (SELECT id FROM customer WHERE code='CUST-2024-0012');
SET @c_2024_0013 := (SELECT id FROM customer WHERE code='CUST-2024-0013');
SET @c_2024_0014 := (SELECT id FROM customer WHERE code='CUST-2024-0014');
SET @c_2024_0015 := (SELECT id FROM customer WHERE code='CUST-2024-0015');
SET @c_2024_0016 := (SELECT id FROM customer WHERE code='CUST-2024-0016');
SET @c_2024_0017 := (SELECT id FROM customer WHERE code='CUST-2024-0017');
SET @c_2024_0018 := (SELECT id FROM customer WHERE code='CUST-2024-0018');
SET @c_2024_0019 := (SELECT id FROM customer WHERE code='CUST-2024-0019');
SET @c_2024_0020 := (SELECT id FROM customer WHERE code='CUST-2024-0020');
SET @c_2024_0021 := (SELECT id FROM customer WHERE code='CUST-2024-0021');
SET @c_2024_0022 := (SELECT id FROM customer WHERE code='CUST-2024-0022');
SET @c_2024_0023 := (SELECT id FROM customer WHERE code='CUST-2024-0023');
SET @c_2024_0024 := (SELECT id FROM customer WHERE code='CUST-2024-0024');
SET @c_2024_0025 := (SELECT id FROM customer WHERE code='CUST-2024-0025');
SET @c_2024_0026 := (SELECT id FROM customer WHERE code='CUST-2024-0026');
SET @c_2024_0027 := (SELECT id FROM customer WHERE code='CUST-2024-0027');
SET @c_2024_0028 := (SELECT id FROM customer WHERE code='CUST-2024-0028');
SET @c_2024_0029 := (SELECT id FROM customer WHERE code='CUST-2024-0029');
SET @c_2024_0030 := (SELECT id FROM customer WHERE code='CUST-2024-0030');
SET @c_2024_0031 := (SELECT id FROM customer WHERE code='CUST-2024-0031');
SET @c_2024_0032 := (SELECT id FROM customer WHERE code='CUST-2024-0032');
SET @c_2024_0033 := (SELECT id FROM customer WHERE code='CUST-2024-0033');
SET @c_2024_0034 := (SELECT id FROM customer WHERE code='CUST-2024-0034');
SET @c_2024_0035 := (SELECT id FROM customer WHERE code='CUST-2024-0035');
SET @c_2024_0036 := (SELECT id FROM customer WHERE code='CUST-2024-0036');
SET @c_2024_0037 := (SELECT id FROM customer WHERE code='CUST-2024-0037');
SET @c_2024_0038 := (SELECT id FROM customer WHERE code='CUST-2024-0038');
SET @c_2024_0039 := (SELECT id FROM customer WHERE code='CUST-2024-0039');
SET @c_2024_0040 := (SELECT id FROM customer WHERE code='CUST-2024-0040');
SET @c_2024_0041 := (SELECT id FROM customer WHERE code='CUST-2024-0041');
SET @c_2024_0042 := (SELECT id FROM customer WHERE code='CUST-2024-0042');
SET @c_2024_0043 := (SELECT id FROM customer WHERE code='CUST-2024-0043');
SET @c_2024_0044 := (SELECT id FROM customer WHERE code='CUST-2024-0044');
SET @c_2024_0045 := (SELECT id FROM customer WHERE code='CUST-2024-0045');
SET @c_2025_0001 := (SELECT id FROM customer WHERE code='CUST-2025-0001');
SET @c_2025_0002 := (SELECT id FROM customer WHERE code='CUST-2025-0002');
SET @c_2025_0003 := (SELECT id FROM customer WHERE code='CUST-2025-0003');
SET @c_2025_0004 := (SELECT id FROM customer WHERE code='CUST-2025-0004');
SET @c_2025_0005 := (SELECT id FROM customer WHERE code='CUST-2025-0005');
SET @c_2025_0006 := (SELECT id FROM customer WHERE code='CUST-2025-0006');
SET @c_2025_0007 := (SELECT id FROM customer WHERE code='CUST-2025-0007');
SET @c_2025_0008 := (SELECT id FROM customer WHERE code='CUST-2025-0008');
SET @c_2025_0009 := (SELECT id FROM customer WHERE code='CUST-2025-0009');
SET @c_2025_0010 := (SELECT id FROM customer WHERE code='CUST-2025-0010');
SET @c_2025_0011 := (SELECT id FROM customer WHERE code='CUST-2025-0011');
SET @c_2025_0012 := (SELECT id FROM customer WHERE code='CUST-2025-0012');
SET @c_2025_0013 := (SELECT id FROM customer WHERE code='CUST-2025-0013');
SET @c_2025_0014 := (SELECT id FROM customer WHERE code='CUST-2025-0014');
SET @c_2025_0015 := (SELECT id FROM customer WHERE code='CUST-2025-0015');
SET @c_2025_0016 := (SELECT id FROM customer WHERE code='CUST-2025-0016');
SET @c_2025_0017 := (SELECT id FROM customer WHERE code='CUST-2025-0017');
SET @c_2025_0018 := (SELECT id FROM customer WHERE code='CUST-2025-0018');
SET @c_2025_0019 := (SELECT id FROM customer WHERE code='CUST-2025-0019');
SET @c_2025_0020 := (SELECT id FROM customer WHERE code='CUST-2025-0020');
SET @c_2025_0021 := (SELECT id FROM customer WHERE code='CUST-2025-0021');
SET @c_2025_0022 := (SELECT id FROM customer WHERE code='CUST-2025-0022');
SET @c_2025_0023 := (SELECT id FROM customer WHERE code='CUST-2025-0023');
SET @c_2025_0024 := (SELECT id FROM customer WHERE code='CUST-2025-0024');
SET @c_2025_0025 := (SELECT id FROM customer WHERE code='CUST-2025-0025');
SET @c_2025_0026 := (SELECT id FROM customer WHERE code='CUST-2025-0026');
SET @c_2025_0027 := (SELECT id FROM customer WHERE code='CUST-2025-0027');
SET @c_2025_0028 := (SELECT id FROM customer WHERE code='CUST-2025-0028');
SET @c_2025_0029 := (SELECT id FROM customer WHERE code='CUST-2025-0029');
SET @c_2025_0030 := (SELECT id FROM customer WHERE code='CUST-2025-0030');
SET @c_2025_0031 := (SELECT id FROM customer WHERE code='CUST-2025-0031');
SET @c_2025_0032 := (SELECT id FROM customer WHERE code='CUST-2025-0032');
SET @c_2026_0003 := (SELECT id FROM customer WHERE code='CUST-2026-0003');
SET @c_2026_0004 := (SELECT id FROM customer WHERE code='CUST-2026-0004');
SET @c_2026_0005 := (SELECT id FROM customer WHERE code='CUST-2026-0005');
SET @c_2026_0006 := (SELECT id FROM customer WHERE code='CUST-2026-0006');
SET @c_2026_0007 := (SELECT id FROM customer WHERE code='CUST-2026-0007');
SET @c_2026_0008 := (SELECT id FROM customer WHERE code='CUST-2026-0008');
SET @c_2026_0009 := (SELECT id FROM customer WHERE code='CUST-2026-0009');
SET @c_2026_0010 := (SELECT id FROM customer WHERE code='CUST-2026-0010');
SET @c_2026_0011 := (SELECT id FROM customer WHERE code='CUST-2026-0011');
SET @c_2026_0012 := (SELECT id FROM customer WHERE code='CUST-2026-0012');
SET @c_2026_0013 := (SELECT id FROM customer WHERE code='CUST-2026-0013');
SET @c_2026_0014 := (SELECT id FROM customer WHERE code='CUST-2026-0014');
SET @c_2026_0015 := (SELECT id FROM customer WHERE code='CUST-2026-0015');
SET @c_2026_0016 := (SELECT id FROM customer WHERE code='CUST-2026-0016');
SET @c_2026_0017 := (SELECT id FROM customer WHERE code='CUST-2026-0017');
SET @c_2026_0018 := (SELECT id FROM customer WHERE code='CUST-2026-0018');
SET @c_2026_0019 := (SELECT id FROM customer WHERE code='CUST-2026-0019');
SET @c_2026_0020 := (SELECT id FROM customer WHERE code='CUST-2026-0020');
SET @c_2026_0021 := (SELECT id FROM customer WHERE code='CUST-2026-0021');
SET @c_2026_0022 := (SELECT id FROM customer WHERE code='CUST-2026-0022');
SET @c_2026_0023 := (SELECT id FROM customer WHERE code='CUST-2026-0023');
SET @v_2026_0001 := (SELECT id FROM vendor WHERE code='VEND-2026-0001');
SET @v_2024_0001 := (SELECT id FROM vendor WHERE code='VEND-2024-0001');
SET @v_2024_0002 := (SELECT id FROM vendor WHERE code='VEND-2024-0002');
SET @v_2024_0003 := (SELECT id FROM vendor WHERE code='VEND-2024-0003');
SET @v_2024_0004 := (SELECT id FROM vendor WHERE code='VEND-2024-0004');
SET @v_2024_0005 := (SELECT id FROM vendor WHERE code='VEND-2024-0005');
SET @v_2024_0006 := (SELECT id FROM vendor WHERE code='VEND-2024-0006');
SET @v_2024_0007 := (SELECT id FROM vendor WHERE code='VEND-2024-0007');
SET @v_2024_0008 := (SELECT id FROM vendor WHERE code='VEND-2024-0008');
SET @v_2024_0009 := (SELECT id FROM vendor WHERE code='VEND-2024-0009');
SET @v_2024_0010 := (SELECT id FROM vendor WHERE code='VEND-2024-0010');
SET @v_2024_0011 := (SELECT id FROM vendor WHERE code='VEND-2024-0011');
SET @v_2024_0012 := (SELECT id FROM vendor WHERE code='VEND-2024-0012');
SET @v_2024_0013 := (SELECT id FROM vendor WHERE code='VEND-2024-0013');
SET @v_2024_0014 := (SELECT id FROM vendor WHERE code='VEND-2024-0014');
SET @v_2024_0015 := (SELECT id FROM vendor WHERE code='VEND-2024-0015');
SET @v_2024_0016 := (SELECT id FROM vendor WHERE code='VEND-2024-0016');
SET @v_2024_0017 := (SELECT id FROM vendor WHERE code='VEND-2024-0017');

-- STEP5 종료 시점 WH-HQ 재고 확정 (stock_movement 누적과 일치)
INSERT INTO stock (item_id,warehouse_id,qty_on_hand,average_cost,version,created_at,created_by,updated_at,updated_by) VALUES
    (@i_2026_0001,@wh,16.0000,693945.90,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0002,@wh,104.0000,409683.37,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0013,@wh,148.0000,782245.87,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0014,@wh,49.0000,1626166.51,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0015,@wh,111.0000,289737.78,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0016,@wh,22.0000,525971.45,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0017,@wh,14.0000,531933.39,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0018,@wh,30.0000,1291997.35,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0019,@wh,6.0000,914585.42,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0020,@wh,19.0000,589985.12,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0021,@wh,2.0000,762486.42,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0022,@wh,2692.0000,14181.83,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0023,@wh,1071.0000,26012.43,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0024,@wh,1262.0000,21867.53,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0025,@wh,271.0000,22260.28,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0026,@wh,482.0000,71791.46,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0003,@wh,302.0000,205385.65,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0004,@wh,248.0000,155927.19,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0005,@wh,64.0000,47594.03,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0006,@wh,102.0000,157205.67,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0007,@wh,38.0000,80929.55,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0008,@wh,749.0000,242324.88,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0009,@wh,2554.0000,79882.06,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0010,@wh,796.0000,41088.53,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0011,@wh,1227.0000,28901.90,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0012,@wh,1049.0000,19782.93,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0027,@wh,859.0000,187889.93,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0028,@wh,135.0000,307215.24,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0029,@wh,973.0000,120159.73,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0030,@wh,55.0000,356130.69,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0031,@wh,465.0000,91209.34,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0032,@wh,254.0000,140056.49,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0033,@wh,34.0000,128882.68,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0034,@wh,158.0000,180428.65,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0035,@wh,689.0000,351135.11,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0036,@wh,468.0000,291277.45,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0037,@wh,374.0000,29076.84,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0038,@wh,219.0000,86875.79,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0039,@wh,151.0000,59594.63,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0040,@wh,201.0000,260783.89,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0041,@wh,559.0000,58239.90,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0042,@wh,1759.0000,25050.83,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0043,@wh,12512.0000,8240.72,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0044,@wh,420.0000,67075.79,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0045,@wh,434.0000,47973.84,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0046,@wh,728.0000,114981.94,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0047,@wh,4716.0000,5964.30,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0048,@wh,14510.0000,11920.42,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0049,@wh,6514.0000,8699.81,0,NOW(), 'system', NOW(), 'system'),
    (@i_2026_0050,@wh,807.0000,4930.45,0,NOW(), 'system', NOW(), 'system')
ON DUPLICATE KEY UPDATE qty_on_hand=VALUES(qty_on_hand), average_cost=VALUES(average_cost), updated_at=NOW();
