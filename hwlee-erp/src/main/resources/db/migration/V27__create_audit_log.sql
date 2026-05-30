-- V27: 감사 로그 (Phase 6, 층위 ②)
-- 변경 한 건마다 한 행(append-only). StockMovement(재고 원장)/JournalEntry(회계 원장)와 같은 패턴.
-- 자기 자신은 감사하지 않으므로 BaseEntity 를 상속하지 않고 직접 컬럼을 가진다(무한 루프 차단).

CREATE TABLE audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    entity_type VARCHAR(100) NOT NULL COMMENT '대상 엔티티 단순명 (예: Customer)',
    entity_id   BIGINT       NOT NULL COMMENT '대상 PK',
    action      VARCHAR(10)  NOT NULL COMMENT 'INSERT/UPDATE/DELETE',
    changed_by  VARCHAR(64)  NOT NULL COMMENT '변경자 (AuditorAware 와 동일 소스)',
    changed_at  DATETIME     NOT NULL COMMENT '변경 시각',
    changes     LONGTEXT              COMMENT '변경 내용 스냅샷(JSON 문자열)',
    PRIMARY KEY (id),
    KEY idx_audit_log_entity (entity_type, entity_id),
    KEY idx_audit_log_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='감사 로그 (변경 이력)';
