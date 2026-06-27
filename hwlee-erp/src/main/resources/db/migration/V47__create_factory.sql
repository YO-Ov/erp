-- STEP 3 (실무 리얼리즘 확장): 공장(생산 사업장) 마스터 신설 + 창고를 공장에 소속.
--
-- 기존엔 "장소"가 창고(warehouse) 하나뿐이라 공장 개념이 없었다. 다공장(3개) 제조사를 표현하기 위해
-- factory 마스터를 만들고, warehouse.factory_id 로 창고를 공장에 묶는다. 생산지시(production_order)는
-- 자신의 창고(warehouse)를 통해 공장에 연결되므로 별도 컬럼을 두지 않는다(정규화).

CREATE TABLE factory (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(30)  NOT NULL                COMMENT '공장코드 FAC-XX (명시적 부여)',
    name       VARCHAR(200) NOT NULL                COMMENT '공장명',
    address    VARCHAR(500)                         COMMENT '소재지',
    status     VARCHAR(16)  NOT NULL                COMMENT 'ACTIVE/INACTIVE/BLOCKED',
    created_at DATETIME     NOT NULL,
    created_by VARCHAR(64)  NOT NULL,
    updated_at DATETIME     NOT NULL,
    updated_by VARCHAR(64)  NOT NULL,
    deleted_at DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_factory_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공장 마스터 (창고·생산설비의 소속 거점)';

-- 3개 공장 시드 (국내 전자 제조 거점 모델).
INSERT INTO factory (code, name, address, status, created_at, created_by, updated_at, updated_by) VALUES
    ('FAC-01', '수원공장', '경기도 수원시 영통구 삼성로 129', 'ACTIVE', NOW(), 'system', NOW(), 'system'),
    ('FAC-02', '구미공장', '경상북도 구미시 1공단로 197',     'ACTIVE', NOW(), 'system', NOW(), 'system'),
    ('FAC-03', '광주공장', '광주광역시 광산구 하남산단6번로 107', 'ACTIVE', NOW(), 'system', NOW(), 'system');

-- 창고에 소속 공장 연결 (nullable — 본사/물류 창고는 공장 미소속 가능).
ALTER TABLE warehouse
    ADD COLUMN factory_id BIGINT NULL COMMENT '소속 공장' AFTER address,
    ADD CONSTRAINT fk_warehouse_factory FOREIGN KEY (factory_id) REFERENCES factory (id);

-- 기존 본사창고(WH-HQ)는 현재 생산이 일어나는 곳이므로 수원공장에 소속시킨다
-- (STEP 4 에서 공장별 창고를 추가하며 재배치). 생산지시→창고→공장 파생이 동작하도록.
UPDATE warehouse
   SET factory_id = (SELECT id FROM factory WHERE code = 'FAC-01')
 WHERE code = 'WH-HQ';
