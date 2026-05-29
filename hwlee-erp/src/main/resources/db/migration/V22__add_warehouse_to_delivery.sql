-- Phase 4: Delivery 가 출하 창고를 직접 안다 (DeliveryShippedEvent 본문에 담아 MM 으로 전달).
--
-- Phase 2 모델은 출하지 창고 개념이 없었다 — 출하가 어느 창고에서 나가는지 알 수 없으면
-- MM 의 재고 차감 대상 창고를 특정할 수 없으므로, Phase 4 에서 보강한다.
--
-- 모듈 경계: JPA 는 Long warehouse_id 로만 매핑한다 (SD 가 MM 의 Warehouse 엔티티를 import 하지 않음).
-- 정합성은 아래 DB FK 제약이 강제하고, 존재 검증은 출하 시점에 MM 리스너가 수행한다.

-- 1) 먼저 nullable 로 추가 (기존 행이 있어도 ALTER 가 실패하지 않도록)
ALTER TABLE delivery
    ADD COLUMN warehouse_id BIGINT NULL COMMENT '출하지 창고';

-- 2) 기존 행(Phase 2 시연 중 생성됐을 수 있음)은 본사창고로 backfill (학습 환경 가정)
UPDATE delivery
   SET warehouse_id = (SELECT id FROM warehouse WHERE code = 'WH-HQ')
 WHERE warehouse_id IS NULL;

-- 3) NOT NULL 강제 — 이후 모든 출하는 창고를 반드시 가진다
ALTER TABLE delivery
    MODIFY COLUMN warehouse_id BIGINT NOT NULL COMMENT '출하지 창고';

ALTER TABLE delivery
    ADD CONSTRAINT fk_delivery_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse(id);

CREATE INDEX idx_delivery_warehouse ON delivery(warehouse_id);
