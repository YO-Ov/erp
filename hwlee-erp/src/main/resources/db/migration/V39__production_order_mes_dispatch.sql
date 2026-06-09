-- Phase 12: 생산지시 → MES 작업지시 전송 추적 컬럼.
-- 전송 전이면 NULL. 멱등 재전송 시 같은 값으로 갱신될 수 있다.

ALTER TABLE production_order
    ADD COLUMN mes_work_order_no VARCHAR(30) NULL COMMENT 'MES 작업지시번호(WO-XXX)' AFTER completed_at,
    ADD COLUMN mes_dispatched_at DATETIME    NULL COMMENT 'MES 전송 시각'           AFTER mes_work_order_no;
