package com.hwlee.erp.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

/**
 * JPA 엔티티 생명주기 콜백으로 변경을 가로채는 리스너 (층위 ②).
 *
 * <p>JPA 가 직접 인스턴스화하므로 Spring 빈이 아니다 → DB 기록을 여기서 직접 하지 않고
 * {@link AuditBuffer} (정적 진입점)에 사건만 적재한다. 실제 기록은 트랜잭션 종료 시점에
 * {@link AuditLogWriter} 가 수행한다.
 *
 * <p>스냅샷 직렬화는 <b>이 콜백 안(세션 열림)에서 즉시</b> 수행한다 — 커밋 이후로 미루면
 * 세션이 닫혀 lazy 프록시 접근에서 깨질 수 있기 때문(설계 §6-2 절충).
 */
public class AuditEntityListener {

    // 콜백은 Spring 빈이 아니므로 정적으로 보관한 ObjectMapper 사용 (AuditBufferConfigurer 가 주입).
    private static final ObjectMapper FALLBACK_MAPPER = new ObjectMapper();

    @PostPersist
    public void onInsert(Object entity) {
        emit(entity, AuditAction.INSERT);
    }

    @PostUpdate
    public void onUpdate(Object entity) {
        emit(entity, AuditAction.UPDATE);
    }

    @PostRemove
    public void onDelete(Object entity) {
        emit(entity, AuditAction.DELETE);
    }

    private void emit(Object entity, AuditAction action) {
        if (entity instanceof Auditable auditable) {
            String json = toJson(auditable.auditSnapshot());
            AuditBuffer.add(new AuditEvent(
                    auditable.auditEntityType(),
                    auditable.getId(),
                    action,
                    json));
        }
    }

    private String toJson(Object snapshot) {
        if (snapshot == null) {
            return null;
        }
        ObjectMapper mapper = AuditBuffer.objectMapper();
        if (mapper == null) {
            mapper = FALLBACK_MAPPER;
        }
        try {
            return mapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            return null;
        }
    }
}
