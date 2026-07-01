-- STEP 4 (실무 리얼리즘 확장): 창고 확장 (중앙물류 WH-HQ 재배정 + 공장창고 3 신설)
-- WH-HQ 는 공장 미소속(중앙물류)로 되돌리고, 공장별 창고 3개를 각 공장에 소속시킨다.

-- 1) 본사중앙창고: 공장 미소속(중앙물류 거점)으로 재배정. 이름도 명확화.
UPDATE warehouse SET factory_id = NULL, name = '본사중앙창고', updated_at = NOW(), updated_by = 'system' WHERE code = 'WH-HQ';

-- 2) 공장별 창고 3개 신설 (각 공장에 소속).
INSERT INTO warehouse (code, name, address, factory_id, status, created_at, created_by, updated_at, updated_by, deleted_at)
SELECT 'WH-SW', '수원공장창고', '경기도 수원시 영통구 삼성로 129', f.id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM factory f WHERE f.code = 'FAC-01';

INSERT INTO warehouse (code, name, address, factory_id, status, created_at, created_by, updated_at, updated_by, deleted_at)
SELECT 'WH-GM', '구미공장창고', '경상북도 구미시 1공단로 197', f.id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM factory f WHERE f.code = 'FAC-02';

INSERT INTO warehouse (code, name, address, factory_id, status, created_at, created_by, updated_at, updated_by, deleted_at)
SELECT 'WH-GJ', '광주공장창고', '광주광역시 광산구 하남산단6번로 107', f.id, 'ACTIVE', NOW(), 'system', NOW(), 'system', NULL
  FROM factory f WHERE f.code = 'FAC-03';

