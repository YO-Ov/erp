-- Phase 15: 설비 상태 + 상태 변경 이력(가동률 계산용).

ALTER TABLE equipment
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'IDLE'
        COMMENT 'RUNNING/IDLE/DOWN/MAINTENANCE' AFTER line_name;

CREATE TABLE equipment_status_log (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    equipment_id BIGINT      NOT NULL,
    status       VARCHAR(16) NOT NULL,
    started_at   DATETIME    NOT NULL                COMMENT '이 상태 진입 시각',
    ended_at     DATETIME                            COMMENT '다음 상태로 전이 시각(진행중이면 NULL)',
    created_at   DATETIME    NOT NULL,
    updated_at   DATETIME    NOT NULL,
    PRIMARY KEY (id),
    KEY idx_esl_equipment (equipment_id),
    CONSTRAINT fk_esl_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='설비 상태 변경 이력';
