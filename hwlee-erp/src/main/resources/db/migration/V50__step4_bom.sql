-- STEP 4 (실무 리얼리즘 확장): 완제품 BOM 14종 (기존 노트북15/모니터27 은 유지)
-- 부품명(name)으로 JOIN — 부품/완제품 이름 유일성 전제(V49). 완제품 1대당 소요량.

INSERT INTO bom (product_item_id, component_item_id, quantity, created_at, created_by, updated_at, updated_by)
SELECT p.id, c.id, q.qty, NOW(), 'system', NOW(), 'system'
  FROM item p
  JOIN (
        SELECT 'LCD 패널 13"' AS cname, 1 AS qty
        UNION ALL SELECT '메인보드' AS cname, 1 AS qty
        UNION ALL SELECT 'CPU 모듈(보급형)' AS cname, 1 AS qty
        UNION ALL SELECT '메모리 8GB' AS cname, 1 AS qty
        UNION ALL SELECT 'SSD 256GB' AS cname, 1 AS qty
        UNION ALL SELECT '배터리팩' AS cname, 1 AS qty
        UNION ALL SELECT '노트북 케이스' AS cname, 1 AS qty
        UNION ALL SELECT '전원어댑터' AS cname, 1 AS qty
        UNION ALL SELECT '내부 케이블세트' AS cname, 1 AS qty
       ) q ON 1 = 1
  JOIN item c ON c.name = q.cname AND c.item_type = 'COMPONENT'
 WHERE p.name = 'hyunwoo 노트북 비즈니스 13"' AND p.item_type = 'FINISHED';

INSERT INTO bom (product_item_id, component_item_id, quantity, created_at, created_by, updated_at, updated_by)
SELECT p.id, c.id, q.qty, NOW(), 'system', NOW(), 'system'
  FROM item p
  JOIN (
        SELECT 'LCD 패널 17"' AS cname, 1 AS qty
        UNION ALL SELECT '메인보드' AS cname, 1 AS qty
        UNION ALL SELECT 'CPU 모듈(고성능)' AS cname, 1 AS qty
        UNION ALL SELECT 'GPU 모듈' AS cname, 1 AS qty
        UNION ALL SELECT '메모리 32GB' AS cname, 1 AS qty
        UNION ALL SELECT 'SSD 1TB' AS cname, 1 AS qty
        UNION ALL SELECT '배터리팩' AS cname, 1 AS qty
        UNION ALL SELECT '노트북 케이스' AS cname, 1 AS qty
        UNION ALL SELECT '전원어댑터' AS cname, 1 AS qty
       ) q ON 1 = 1
  JOIN item c ON c.name = q.cname AND c.item_type = 'COMPONENT'
 WHERE p.name = 'hyunwoo 노트북 프로 17"' AND p.item_type = 'FINISHED';

INSERT INTO bom (product_item_id, component_item_id, quantity, created_at, created_by, updated_at, updated_by)
SELECT p.id, c.id, q.qty, NOW(), 'system', NOW(), 'system'
  FROM item p
  JOIN (
        SELECT 'LCD 패널 24"' AS cname, 1 AS qty
        UNION ALL SELECT '스케일러 보드' AS cname, 1 AS qty
        UNION ALL SELECT '전원 보드' AS cname, 1 AS qty
        UNION ALL SELECT '모니터 스탠드' AS cname, 1 AS qty
        UNION ALL SELECT '후면 케이스' AS cname, 1 AS qty
       ) q ON 1 = 1
  JOIN item c ON c.name = q.cname AND c.item_type = 'COMPONENT'
 WHERE p.name = 'hyunwoo 모니터 24"' AND p.item_type = 'FINISHED';

INSERT INTO bom (product_item_id, component_item_id, quantity, created_at, created_by, updated_at, updated_by)
SELECT p.id, c.id, q.qty, NOW(), 'system', NOW(), 'system'
  FROM item p
  JOIN (
        SELECT 'LCD 패널 32"' AS cname, 1 AS qty
        UNION ALL SELECT '스케일러 보드' AS cname, 1 AS qty
        UNION ALL SELECT '전원 보드' AS cname, 1 AS qty
        UNION ALL SELECT '모니터 스탠드' AS cname, 1 AS qty
        UNION ALL SELECT '후면 케이스' AS cname, 1 AS qty
       ) q ON 1 = 1
  JOIN item c ON c.name = q.cname AND c.item_type = 'COMPONENT'
 WHERE p.name = 'hyunwoo 모니터 32" 커브드' AND p.item_type = 'FINISHED';

INSERT INTO bom (product_item_id, component_item_id, quantity, created_at, created_by, updated_at, updated_by)
SELECT p.id, c.id, q.qty, NOW(), 'system', NOW(), 'system'
  FROM item p
  JOIN (
        SELECT '데스크탑 메인보드' AS cname, 1 AS qty
        UNION ALL SELECT 'CPU 모듈(보급형)' AS cname, 1 AS qty
        UNION ALL SELECT '16GB 메모리' AS cname, 1 AS qty
        UNION ALL SELECT 'SSD 256GB' AS cname, 1 AS qty
        UNION ALL SELECT '데스크탑 케이스' AS cname, 1 AS qty
        UNION ALL SELECT '전원 보드' AS cname, 1 AS qty
        UNION ALL SELECT '전원어댑터' AS cname, 1 AS qty
       ) q ON 1 = 1
  JOIN item c ON c.name = q.cname AND c.item_type = 'COMPONENT'
 WHERE p.name = 'hyunwoo 데스크탑 사무용' AND p.item_type = 'FINISHED';

INSERT INTO bom (product_item_id, component_item_id, quantity, created_at, created_by, updated_at, updated_by)
SELECT p.id, c.id, q.qty, NOW(), 'system', NOW(), 'system'
  FROM item p
  JOIN (
        SELECT '데스크탑 메인보드' AS cname, 1 AS qty
        UNION ALL SELECT 'CPU 모듈(고성능)' AS cname, 1 AS qty
        UNION ALL SELECT 'GPU 모듈' AS cname, 1 AS qty
        UNION ALL SELECT '메모리 32GB' AS cname, 2 AS qty
        UNION ALL SELECT 'SSD 1TB' AS cname, 1 AS qty
        UNION ALL SELECT '데스크탑 케이스' AS cname, 1 AS qty
        UNION ALL SELECT '전원 보드' AS cname, 1 AS qty
       ) q ON 1 = 1
  JOIN item c ON c.name = q.cname AND c.item_type = 'COMPONENT'
 WHERE p.name = 'hyunwoo 워크스테이션' AND p.item_type = 'FINISHED';

INSERT INTO bom (product_item_id, component_item_id, quantity, created_at, created_by, updated_at, updated_by)
SELECT p.id, c.id, q.qty, NOW(), 'system', NOW(), 'system'
  FROM item p
  JOIN (
        SELECT '27" LCD 패널' AS cname, 1 AS qty
        UNION ALL SELECT '데스크탑 메인보드' AS cname, 1 AS qty
        UNION ALL SELECT 'CPU 모듈(보급형)' AS cname, 1 AS qty
        UNION ALL SELECT '16GB 메모리' AS cname, 1 AS qty
        UNION ALL SELECT '512GB SSD' AS cname, 1 AS qty
        UNION ALL SELECT 'AIO 케이스' AS cname, 1 AS qty
        UNION ALL SELECT '전원 보드' AS cname, 1 AS qty
       ) q ON 1 = 1
  JOIN item c ON c.name = q.cname AND c.item_type = 'COMPONENT'
 WHERE p.name = 'hyunwoo 올인원PC 27"' AND p.item_type = 'FINISHED';

INSERT INTO bom (product_item_id, component_item_id, quantity, created_at, created_by, updated_at, updated_by)
SELECT p.id, c.id, q.qty, NOW(), 'system', NOW(), 'system'
  FROM item p
  JOIN (
        SELECT 'LCD 패널 10"(태블릿)' AS cname, 1 AS qty
        UNION ALL SELECT '메인보드' AS cname, 1 AS qty
        UNION ALL SELECT 'CPU 모듈(보급형)' AS cname, 1 AS qty
        UNION ALL SELECT '메모리 8GB' AS cname, 1 AS qty
        UNION ALL SELECT 'SSD 256GB' AS cname, 1 AS qty
        UNION ALL SELECT '태블릿 배터리' AS cname, 1 AS qty
        UNION ALL SELECT '전원어댑터' AS cname, 1 AS qty
       ) q ON 1 = 1
  JOIN item c ON c.name = q.cname AND c.item_type = 'COMPONENT'
 WHERE p.name = 'hyunwoo 태블릿 10"' AND p.item_type = 'FINISHED';

INSERT INTO bom (product_item_id, component_item_id, quantity, created_at, created_by, updated_at, updated_by)
SELECT p.id, c.id, q.qty, NOW(), 'system', NOW(), 'system'
  FROM item p
  JOIN (
        SELECT 'LCD 패널 12"(태블릿)' AS cname, 1 AS qty
        UNION ALL SELECT '메인보드' AS cname, 1 AS qty
        UNION ALL SELECT 'CPU 모듈(보급형)' AS cname, 1 AS qty
        UNION ALL SELECT '16GB 메모리' AS cname, 1 AS qty
        UNION ALL SELECT '512GB SSD' AS cname, 1 AS qty
        UNION ALL SELECT '태블릿 배터리' AS cname, 1 AS qty
        UNION ALL SELECT '전원어댑터' AS cname, 1 AS qty
       ) q ON 1 = 1
  JOIN item c ON c.name = q.cname AND c.item_type = 'COMPONENT'
 WHERE p.name = 'hyunwoo 태블릿 12"' AND p.item_type = 'FINISHED';

INSERT INTO bom (product_item_id, component_item_id, quantity, created_at, created_by, updated_at, updated_by)
SELECT p.id, c.id, q.qty, NOW(), 'system', NOW(), 'system'
  FROM item p
  JOIN (
        SELECT '키캡세트' AS cname, 1 AS qty
        UNION ALL SELECT '내부 케이블세트' AS cname, 1 AS qty
       ) q ON 1 = 1
  JOIN item c ON c.name = q.cname AND c.item_type = 'COMPONENT'
 WHERE p.name = 'hyunwoo 멤브레인 키보드' AND p.item_type = 'FINISHED';

INSERT INTO bom (product_item_id, component_item_id, quantity, created_at, created_by, updated_at, updated_by)
SELECT p.id, c.id, q.qty, NOW(), 'system', NOW(), 'system'
  FROM item p
  JOIN (
        SELECT '키캡세트' AS cname, 1 AS qty
        UNION ALL SELECT '기계식 스위치' AS cname, 1 AS qty
        UNION ALL SELECT '내부 케이블세트' AS cname, 1 AS qty
       ) q ON 1 = 1
  JOIN item c ON c.name = q.cname AND c.item_type = 'COMPONENT'
 WHERE p.name = 'hyunwoo 기계식 키보드' AND p.item_type = 'FINISHED';

INSERT INTO bom (product_item_id, component_item_id, quantity, created_at, created_by, updated_at, updated_by)
SELECT p.id, c.id, q.qty, NOW(), 'system', NOW(), 'system'
  FROM item p
  JOIN (
        SELECT '광센서 모듈' AS cname, 1 AS qty
        UNION ALL SELECT '마우스 하우징' AS cname, 1 AS qty
        UNION ALL SELECT '내부 케이블세트' AS cname, 1 AS qty
       ) q ON 1 = 1
  JOIN item c ON c.name = q.cname AND c.item_type = 'COMPONENT'
 WHERE p.name = 'hyunwoo 무선 마우스' AND p.item_type = 'FINISHED';

INSERT INTO bom (product_item_id, component_item_id, quantity, created_at, created_by, updated_at, updated_by)
SELECT p.id, c.id, q.qty, NOW(), 'system', NOW(), 'system'
  FROM item p
  JOIN (
        SELECT '광센서 모듈' AS cname, 1 AS qty
        UNION ALL SELECT '마우스 하우징' AS cname, 1 AS qty
        UNION ALL SELECT '내부 케이블세트' AS cname, 1 AS qty
       ) q ON 1 = 1
  JOIN item c ON c.name = q.cname AND c.item_type = 'COMPONENT'
 WHERE p.name = 'hyunwoo 게이밍 마우스' AND p.item_type = 'FINISHED';

INSERT INTO bom (product_item_id, component_item_id, quantity, created_at, created_by, updated_at, updated_by)
SELECT p.id, c.id, q.qty, NOW(), 'system', NOW(), 'system'
  FROM item p
  JOIN (
        SELECT '전원 보드' AS cname, 1 AS qty
        UNION ALL SELECT '내부 케이블세트' AS cname, 1 AS qty
        UNION ALL SELECT '전원어댑터' AS cname, 1 AS qty
       ) q ON 1 = 1
  JOIN item c ON c.name = q.cname AND c.item_type = 'COMPONENT'
 WHERE p.name = 'hyunwoo USB-C 도킹스테이션' AND p.item_type = 'FINISHED';

