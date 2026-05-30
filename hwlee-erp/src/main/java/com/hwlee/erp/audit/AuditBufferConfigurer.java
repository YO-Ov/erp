package com.hwlee.erp.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JPA 엔티티 리스너는 Spring 빈이 아니므로, 정적 {@link AuditBuffer} 에 협력 빈
 * ({@link AuditLogWriter}, {@link ObjectMapper})을 주입(브리지)하는 역할. 기동 시 한 번 연결.
 */
@Component
@RequiredArgsConstructor
public class AuditBufferConfigurer {

    private final AuditLogWriter auditLogWriter;
    private final ObjectMapper objectMapper;

    @PostConstruct
    void wire() {
        AuditBuffer.configure(auditLogWriter, objectMapper);
    }
}
