-- STEP 3 (실무 리얼리즘 확장): 설비를 공장에 소속.
--
-- MES 는 ERP 와 DB 가 분리돼 있어 공유 FK 를 둘 수 없다. 시스템 간 연계 관례대로 코드(factory_code)로
-- ERP 의 factory.code 를 가리킨다. 기존 설비(노트북 조립/검사 라인)는 수원공장(FAC-01) 소속으로 둔다.

ALTER TABLE equipment
    ADD COLUMN factory_code VARCHAR(30) NULL COMMENT '소속 공장 코드 (ERP factory.code)' AFTER line_name;

UPDATE equipment SET factory_code = 'FAC-01' WHERE code IN ('EQ-001', 'EQ-002');
