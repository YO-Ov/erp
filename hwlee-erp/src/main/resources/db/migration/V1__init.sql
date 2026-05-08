-- Phase 0: 스키마 마이그레이션 동작 확인용 더미 테이블
-- Phase 1에서 실제 마스터 테이블이 추가되면서 V2부터 이어진다.

CREATE TABLE schema_init_marker (
    id BIGINT NOT NULL AUTO_INCREMENT,
    note VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO schema_init_marker(note) VALUES ('Phase 0 init OK');
