package com.hwlee.erp.audit;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 감사 로그 — 변경 한 건 = 한 행 (append-only).
 *
 * <p>Phase 3 {@code StockMovement}(재고 원장), Phase 5 {@code JournalEntry}(회계 원장)와 같은 패턴 —
 * "진실은 이력의 누적". setter 없음, 한 번 저장되면 수정/삭제되지 않는다.
 *
 * <p>자기 자신은 감사 대상이 아니므로 {@code BaseEntity}/{@code Auditable} 을 상속/구현하지 않는다
 * (무한 루프 차단). created_by 류 컬럼도 두지 않고 changed_by/changed_at 만 직접 가진다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 100, updatable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false, updatable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 10, updatable = false)
    private AuditAction action;

    @Column(name = "changed_by", nullable = false, length = 64, updatable = false)
    private String changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @Column(name = "changes", updatable = false)
    private String changes;

    public static AuditLog of(String entityType, Long entityId, AuditAction action,
                              String changedBy, LocalDateTime changedAt, String changes) {
        AuditLog log = new AuditLog();
        log.entityType = entityType;
        log.entityId = entityId;
        log.action = action;
        log.changedBy = changedBy;
        log.changedAt = changedAt;
        log.changes = changes;
        return log;
    }
}
