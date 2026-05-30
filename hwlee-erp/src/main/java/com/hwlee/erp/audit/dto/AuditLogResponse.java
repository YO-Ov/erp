package com.hwlee.erp.audit.dto;

import com.hwlee.erp.audit.AuditAction;
import com.hwlee.erp.audit.AuditLog;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String entityType,
        Long entityId,
        AuditAction action,
        String changedBy,
        LocalDateTime changedAt,
        String changes
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                log.getChangedBy(),
                log.getChangedAt(),
                log.getChanges());
    }
}
