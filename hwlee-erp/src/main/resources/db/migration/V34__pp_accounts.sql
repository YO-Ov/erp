-- Phase 8: PP 회계 계정 — 원재료/제품 분리 (현실형 간이 제조회계).
--
-- 기존 재고자산(1400) 잔액은 전부 완제품(노트북/모니터)이므로 1400 을 '제품' 의미로 쓰고
-- 원재료(1410) 만 신설한다(재분류 불필요). 부품 기초재고 GL 상계용 이익잉여금(3100)도 추가.

-- 1) 1400 의 의미를 '제품(재고자산)' 으로 명확화 (코드·용도 유지 → SystemAccounts.INVENTORY 불변).
UPDATE account SET name = '제품(재고자산)', updated_at = NOW(), updated_by = 'system'
 WHERE code = '1400';

-- 2) 원재료(1410) — 자산 말단 계정. 부모 = 자산(1000).
INSERT INTO account (code, name, type, parent_id, postable, status,
                     created_at, created_by, updated_at, updated_by)
SELECT '1410', '원재료', 'ASSET', id, 1, 'ACTIVE', NOW(), 'system', NOW(), 'system'
  FROM account WHERE code = '1000';

-- 3) 자본(3000 헤더) + 이익잉여금(3100) — 기초재고 이월 상계용.
INSERT INTO account (code, name, type, parent_id, postable, status,
                     created_at, created_by, updated_at, updated_by)
VALUES ('3000', '자본', 'EQUITY', NULL, 0, 'ACTIVE', NOW(), 'system', NOW(), 'system');

INSERT INTO account (code, name, type, parent_id, postable, status,
                     created_at, created_by, updated_at, updated_by)
SELECT '3100', '이익잉여금', 'EQUITY', id, 1, 'ACTIVE', NOW(), 'system', NOW(), 'system'
  FROM account WHERE code = '3000';
