package com.hwlee.erp.audit;

import org.springframework.util.ClassUtils;

import java.util.Map;

/**
 * 감사 대상 엔티티가 구현하는 인터페이스 (층위 ② — 변경 이력 로그).
 *
 * <p>선택 B(설계): 전 엔티티가 아니라 감사가 꼭 필요한 핵심 엔티티만 이 인터페이스를 구현하고
 * {@code @EntityListeners(AuditEntityListener.class)} 를 단다. 무엇을 감사 대상으로 삼을지를
 * 의식적으로 고르는 것 자체가 학습 포인트.
 *
 * <p>{@link #auditSnapshot()} 가 "무엇이 바뀌었나" 에 들어갈 필드를 직접 노출한다 —
 * 리플렉션 대신 명시적으로 골라, 감사에 남길 정보를 코드로 분명히 한다.
 */
public interface Auditable {

    /** 대상 식별자 (BaseEntity.getId()). */
    Long getId();

    /** 감사 로그 entity_type 에 기록될 이름. 기본은 Hibernate 프록시를 벗긴 실제 클래스의 단순명. */
    default String auditEntityType() {
        return ClassUtils.getUserClass(this).getSimpleName();
    }

    /** 변경 내용 스냅샷 — JSON 으로 직렬화되어 audit_log.changes 에 저장된다. */
    Map<String, Object> auditSnapshot();
}
