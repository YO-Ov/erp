-- 전자결재 전결 규정 확장: 지급결의(PAYMENT) · 수동전표(JOURNAL).
-- 재무 문서라 재무 합의는 두지 않고(이미 재무 결재 라인) 금액 구간별 결재 레벨만 정의.

INSERT INTO approval_rule (doc_type, min_amount, max_amount, approval_level, require_finance_agreement, created_at, created_by, updated_at, updated_by) VALUES
    -- 지급결의(출금): 1천만 미만 팀장 / 1천만~5천만 본부장 / 5천만 이상 대표
    ('PAYMENT',        0.00, 10000000.00, 'TEAM',     0, NOW(), 'system', NOW(), 'system'),
    ('PAYMENT', 10000000.00, 50000000.00, 'DIVISION', 0, NOW(), 'system', NOW(), 'system'),
    ('PAYMENT', 50000000.00, NULL,        'COMPANY',  0, NOW(), 'system', NOW(), 'system'),
    -- 수동전표: 1천만 미만 팀장 / 1천만 이상 본부장
    ('JOURNAL',        0.00, 10000000.00, 'TEAM',     0, NOW(), 'system', NOW(), 'system'),
    ('JOURNAL', 10000000.00, NULL,        'DIVISION', 0, NOW(), 'system', NOW(), 'system');
