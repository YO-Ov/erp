package com.hwlee.erp.audit;

/**
 * 엔티티 변경 한 건을 표현하는 값. JPA 콜백 시점(세션 열림)에 만들어진다.
 *
 * <p>{@code changesJson} 은 콜백 시점에 <b>이미 JSON 문자열로 직렬화</b>해 담는다 —
 * 직렬화를 트랜잭션 커밋 이후로 미루면 세션이 닫혀 lazy 연관(프록시) 접근에서 깨질 수 있으므로,
 * 스냅샷은 세션이 살아 있는 콜백 안에서 즉시 문자열로 고정한다.
 */
public record AuditEvent(
        String entityType,
        Long entityId,
        AuditAction action,
        String changesJson
) {
}
