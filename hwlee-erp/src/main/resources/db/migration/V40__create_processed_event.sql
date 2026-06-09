-- Phase 14: 멱등 수신 — 처리한 이벤트 ID 를 기록해 중복 메시지를 한 번만 반영한다.
-- Kafka 는 최소 1회 전달이라 같은 메시지가 두 번 올 수 있다. event_id UNIQUE 가 최종 방어선.

CREATE TABLE processed_event (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    event_id     VARCHAR(80) NOT NULL                COMMENT '이벤트 멱등 키',
    source       VARCHAR(30) NOT NULL                COMMENT '출처 시스템 (예: MES)',
    processed_at DATETIME    NOT NULL,
    created_at   DATETIME    NOT NULL,
    created_by   VARCHAR(64) NOT NULL,
    updated_at   DATETIME    NOT NULL,
    updated_by   VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_processed_event (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='멱등 수신 기록';
