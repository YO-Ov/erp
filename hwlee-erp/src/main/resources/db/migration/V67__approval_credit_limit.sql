-- 여신 상향(CREDIT_LIMIT)의 전자결재 통합: 고정 결재 부서(재무팀) 지원 + 규칙 시드.

-- 1) 전결 규정에 "고정 결재 부서" 컬럼 추가 — 지정되면 상신자 조직과 무관하게 그 부서장이 결재.
ALTER TABLE approval_rule
    ADD COLUMN fixed_approver_dept_code VARCHAR(30) NULL
        COMMENT '고정 결재 부서 코드(지정 시 조직 상향 대신 그 부서장이 결재. 예: 여신=DEPT-FINANCE)'
        AFTER require_finance_agreement;

-- 2) 여신 상향 규칙 — 금액 무관, 재무팀장(DEPT-FINANCE 부서장) 단일 결재.
INSERT INTO approval_rule (doc_type, min_amount, max_amount, approval_level, require_finance_agreement, fixed_approver_dept_code, created_at, created_by, updated_at, updated_by) VALUES
    ('CREDIT_LIMIT', 0.00, NULL, 'TEAM', 0, 'DEPT-FINANCE', NOW(), 'system', NOW(), 'system');
