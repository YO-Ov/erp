package com.hwlee.erp.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByEntityTypeAndEntityIdOrderByChangedAtDesc(
            String entityType, Long entityId, Pageable pageable);

    Page<AuditLog> findByEntityTypeOrderByChangedAtDesc(String entityType, Pageable pageable);
}
