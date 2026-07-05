-- 전자결재 참조(REFERENCE) 단계 지원: 전결 규정에 참조 부서 지정.

-- 1) 참조 부서 코드 컬럼 — 지정 시 그 부서장이 결재권 없이 열람·통보만 받는 참조 단계로 붙는다.
ALTER TABLE approval_rule
    ADD COLUMN reference_dept_code VARCHAR(30) NULL
        COMMENT '참조 부서 코드(부서장이 REFERENCE 단계로 열람. 예: 본부장 전결 건에 대표 DEPT-HQ 참조)'
        AFTER fixed_approver_dept_code;

-- 2) 견적 "본부장 전결(1천만~5천만)" 구간에 대표(DEPT-HQ 부서장=admin)를 참조로 지정.
--    → 결재는 팀장·본부장까지지만 대표가 열람·인지한다(상위 보고).
UPDATE approval_rule
   SET reference_dept_code = 'DEPT-HQ', updated_at = NOW(), updated_by = 'system'
 WHERE doc_type = 'QUOTATION' AND approval_level = 'DIVISION';
