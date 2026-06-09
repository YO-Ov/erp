-- Phase 14: Transactional Outbox.
-- 생산 실적 저장과 "보낼 메시지" INSERT 가 같은 트랜잭션에 묶여 원자성을 보장한다.
-- 별도 Publisher 가 PENDING 행을 폴링해 Kafka 로 발행하고 SENT 로 표시한다.

CREATE TABLE outbox_event (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    aggregate_type VARCHAR(50)  NOT NULL                COMMENT '집계 종류 (예: WORK_ORDER)',
    aggregate_id   VARCHAR(50)  NOT NULL                COMMENT '집계 식별자 (예: WO-...)',
    event_type     VARCHAR(50)  NOT NULL                COMMENT '이벤트 종류',
    event_id       VARCHAR(80)  NOT NULL                COMMENT '멱등 키 (수신측 중복 차단)',
    payload        TEXT         NOT NULL                COMMENT '직렬화된 이벤트(JSON)',
    status         VARCHAR(16)  NOT NULL                COMMENT 'PENDING/SENT',
    sent_at        DATETIME                             COMMENT '발행 시각',
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_event_id (event_id),
    KEY idx_outbox_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Transactional Outbox';
