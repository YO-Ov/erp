-- STEP 4 (실무 리얼리즘 확장): MES 설비/작업자 공장별 확장.
-- 공장마다 라인 2개 + 작업자 2명. equipment.factory_code 로 ERP 공장에 귀속.

INSERT INTO equipment (code, name, line_name, factory_code, created_at, updated_at) VALUES
    ('EQ-101', 'SMT 라인 A', '수원 노트북라인', 'FAC-01', NOW(), NOW()),
    ('EQ-102', '조립 라인 A', '수원 AIO라인', 'FAC-01', NOW(), NOW()),
    ('EQ-201', '패널 조립 라인', '구미 모니터라인', 'FAC-02', NOW(), NOW()),
    ('EQ-202', '태블릿 조립 라인', '구미 태블릿라인', 'FAC-02', NOW(), NOW()),
    ('EQ-301', '본체 조립 라인', '광주 데스크탑라인', 'FAC-03', NOW(), NOW()),
    ('EQ-302', '주변기기 라인', '광주 주변기기라인', 'FAC-03', NOW(), NOW());

INSERT INTO operator (code, name, created_at, updated_at) VALUES
    ('OP-101', '수원작업자1', NOW(), NOW()),
    ('OP-102', '수원작업자2', NOW(), NOW()),
    ('OP-201', '구미작업자1', NOW(), NOW()),
    ('OP-202', '구미작업자2', NOW(), NOW()),
    ('OP-301', '광주작업자1', NOW(), NOW()),
    ('OP-302', '광주작업자2', NOW(), NOW());
