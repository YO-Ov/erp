package com.hwlee.erp.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 트랜잭션 단위 감사 사건 버퍼 (정적 진입점).
 *
 * <p>JPA 엔티티 리스너({@link AuditEntityListener})는 Spring 빈이 아니라 정적 메서드로 접근한다.
 * 사건은 ThreadLocal 에 모아 두고, 비즈니스 트랜잭션이 <b>커밋된 직후</b>
 * {@link TransactionSynchronization#afterCommit()} 에서 {@link AuditLogWriter} 가 별도(REQUIRES_NEW)
 * 트랜잭션으로 기록한다.
 *
 * <p>스냅샷은 콜백 시점에 이미 JSON 문자열로 고정되어 들어오므로(세션 열림), 커밋 후 기록 시점엔
 * 더 이상 엔티티/프록시를 건드리지 않는다.
 */
public final class AuditBuffer {

    private static final ThreadLocal<List<AuditEvent>> EVENTS = ThreadLocal.withInitial(ArrayList::new);

    private static AuditLogWriter writer;
    private static ObjectMapper objectMapper;

    private AuditBuffer() {
    }

    static void configure(AuditLogWriter writer, ObjectMapper objectMapper) {
        AuditBuffer.writer = writer;
        AuditBuffer.objectMapper = objectMapper;
    }

    static ObjectMapper objectMapper() {
        return objectMapper;
    }

    static void add(AuditEvent event) {
        // 트랜잭션 밖(예: 단순 테스트) — 즉시 best-effort 기록.
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (writer != null) {
                writer.write(List.of(event));
            }
            return;
        }
        List<AuditEvent> events = EVENTS.get();
        boolean first = events.isEmpty();
        events.add(event);
        if (first) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if (writer != null) {
                        writer.write(new ArrayList<>(EVENTS.get()));
                    }
                }

                @Override
                public void afterCompletion(int status) {
                    EVENTS.remove(); // 커밋/롤백 무관하게 ThreadLocal 정리
                }
            });
        }
    }
}
