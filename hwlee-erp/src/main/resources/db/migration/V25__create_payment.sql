-- Phase 5: 입금/출금(Payment) — OTC 의 마지막 단계.
--
-- 비즈니스 규칙:
--  - number UNIQUE (PAY-YYYYMMDD-NNN).
--  - type=RECEIPT(입금) → customer_id 필수, vendor_id NULL.   분개: 차)현금 / 대)매출채권.
--  - type=DISBURSEMENT(출금) → vendor_id 필수, customer_id NULL. 분개: 차)매입채무 / 대)현금.
--  - status: DRAFT → POSTED 가 확정. (학습 1차 범위에서 취소 생략 — 추후 역분개로 도입 가능)
--  - 자동 분개는 PaymentService.post 가 직접 AutoJournalService 호출.
--    이벤트로 분리하지 않은 이유: 입금은 그 자체가 회계 사건이라 모듈 분리 의의가 약함.

CREATE TABLE payment (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    number        VARCHAR(30)     NOT NULL                COMMENT '예: PAY-20260524-001',
    type          VARCHAR(16)     NOT NULL                COMMENT 'RECEIPT/DISBURSEMENT',
    customer_id   BIGINT                                  COMMENT 'RECEIPT 일 때 필수',
    vendor_id     BIGINT                                  COMMENT 'DISBURSEMENT 일 때 필수',
    amount        DECIMAL(15, 2)  NOT NULL                COMMENT '금액 (> 0)',
    payment_date  DATE            NOT NULL                COMMENT '입출금일',
    status        VARCHAR(16)     NOT NULL                COMMENT 'DRAFT/POSTED',
    posted_at     DATETIME                                COMMENT '확정 시각',
    description   VARCHAR(255)                            COMMENT '메모',
    created_at    DATETIME        NOT NULL,
    created_by    VARCHAR(64)     NOT NULL,
    updated_at    DATETIME        NOT NULL,
    updated_by    VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_number (number),
    KEY idx_payment_customer (customer_id),
    KEY idx_payment_vendor (vendor_id),
    KEY idx_payment_date (payment_date),
    CONSTRAINT fk_payment_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_payment_vendor   FOREIGN KEY (vendor_id)   REFERENCES vendor(id),
    -- type 과 party 의 정합성: RECEIPT 면 customer 만, DISBURSEMENT 면 vendor 만.
    CONSTRAINT chk_payment_party CHECK (
        (type = 'RECEIPT'      AND customer_id IS NOT NULL AND vendor_id   IS NULL) OR
        (type = 'DISBURSEMENT' AND vendor_id   IS NOT NULL AND customer_id IS NULL)
    ),
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='입금/출금 헤더';
