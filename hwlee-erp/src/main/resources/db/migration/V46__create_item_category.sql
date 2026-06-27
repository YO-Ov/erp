-- STEP 2 (실무 리얼리즘 확장): 품목 카테고리를 enum → 코드 마스터로 일반화.
--
-- 기존엔 ItemCategory(자바 enum, NOTEBOOK/MONITOR/PART)라 카테고리를 늘리려면 코드 수정+재배포가 필요했다.
-- 데스크탑/키보드/마우스 등 라인업을 데이터만으로 추가하려면 카테고리를 마스터 테이블로 빼야 한다.
-- item.category 컬럼은 그대로 두되(값=코드), 이제 item_category(code) 를 참조한다(FK 로 무결성 강제).

CREATE TABLE item_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(20)  NOT NULL                COMMENT '카테고리 코드 — item.category 가 이 값을 참조',
    name        VARCHAR(100) NOT NULL                COMMENT '표시명 (노트북/모니터/…)',
    sort_order  INT          NOT NULL DEFAULT 0      COMMENT '드롭다운/목록 정렬 순서',
    status      VARCHAR(16)  NOT NULL                COMMENT 'ACTIVE/INACTIVE/BLOCKED',
    created_at  DATETIME     NOT NULL,
    created_by  VARCHAR(64)  NOT NULL,
    updated_at  DATETIME     NOT NULL,
    updated_by  VARCHAR(64)  NOT NULL,
    deleted_at  DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_item_category_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='품목 카테고리 마스터 (enum 대체)';

-- 기존 enum 3종을 그대로 마스터로 이관 (기존 item 데이터의 category 값과 1:1 매칭).
INSERT INTO item_category (code, name, sort_order, status, created_at, created_by, updated_at, updated_by) VALUES
    ('NOTEBOOK', '노트북', 10, 'ACTIVE', NOW(), 'system', NOW(), 'system'),
    ('MONITOR',  '모니터', 20, 'ACTIVE', NOW(), 'system', NOW(), 'system'),
    ('PART',     '부품',   90, 'ACTIVE', NOW(), 'system', NOW(), 'system');

-- 참조무결성: item.category → item_category.code. 기존 데이터는 위 3종에 모두 매칭되므로 안전.
-- (item.category 에는 이미 idx_item_category 인덱스가 있어 FK 가 이를 재사용한다.)
ALTER TABLE item
    ADD CONSTRAINT fk_item_category FOREIGN KEY (category) REFERENCES item_category (code);
