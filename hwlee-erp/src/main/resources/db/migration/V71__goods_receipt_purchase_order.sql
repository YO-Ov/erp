-- 입고(GoodsReceipt) ↔ 구매발주(PO) 연동 2차 — 입고에 발주 참조를 달아 "발주 대비 입고"를 추적.
-- nullable: 발주 없이 들어온 입고(과거 3년치 시드 V60~V63, 무발주 긴급 입고)는 그대로 NULL 로 유효.
-- 발주 라인별 입고 누계는 이 참조를 역집계해 계산하고, 전량 입고 시 PO 를 CONFIRMED→RECEIVED 로 전이한다.
-- (PurchaseOrderStatus.RECEIVED 는 status VARCHAR 에 enum name 으로 저장되므로 별도 컬럼 변경 불필요.)

ALTER TABLE goods_receipt
    ADD COLUMN purchase_order_id BIGINT NULL COMMENT '발주 참조(있으면 그 발주에 대한 입고)' AFTER warehouse_id,
    ADD KEY idx_gr_purchase_order (purchase_order_id),
    ADD CONSTRAINT fk_gr_purchase_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_order (id);
