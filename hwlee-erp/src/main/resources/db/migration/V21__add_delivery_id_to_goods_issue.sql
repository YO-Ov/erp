-- Phase 4: GoodsIssue 가 자기 출처(Delivery) 를 강한 FK 로 참조.
--
-- 비즈니스 규칙:
--  - 출하 확정 시 DeliveryEventListener 가 자동 생성하는 GI 는 delivery_id 가 채워진다.
--  - 사용자가 직접 등록한 GI(실사 조정/폐기 등)는 delivery_id = NULL.
--  - nullable FK — 두 출처(자동/수동)가 한 테이블에 공존하며, NULL 여부로 구분된다.
--  - JPA 매핑은 Long 컬럼(약한 객체 참조) — MM 이 SD 엔티티 클래스를 import 하지 않도록
--    모듈 경계를 지킨다. 정합성은 아래 DB FK 제약이 강제한다.

ALTER TABLE goods_issue
    ADD COLUMN delivery_id BIGINT NULL COMMENT '출하 자동 발생 시 원천 Delivery (수동 등록은 NULL)';

ALTER TABLE goods_issue
    ADD CONSTRAINT fk_goods_issue_delivery FOREIGN KEY (delivery_id) REFERENCES delivery(id);

CREATE INDEX idx_goods_issue_delivery ON goods_issue(delivery_id);
