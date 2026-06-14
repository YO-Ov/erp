-- 생산 실적의 불량 사유를 자유 텍스트(note)에서 불량코드(defect_reason FK)로 정규화한다.
-- 불량 분석·집계를 하려면 사유가 코드화돼 있어야 하므로, 기존 비고 칸을 불량코드 참조로 바꾼다.
-- defect_reason 마스터(DEF-01~03)는 V5 에서 이미 적재됨.

ALTER TABLE production_result
    ADD COLUMN defect_reason_id BIGINT NULL COMMENT '불량 사유(코드) — 불량 발생 시 선택' AFTER reported_at,
    ADD CONSTRAINT fk_pr_defect_reason FOREIGN KEY (defect_reason_id) REFERENCES defect_reason(id);

-- 비고(자유 텍스트)는 불량코드로 대체하므로 제거.
ALTER TABLE production_result DROP COLUMN note;
