package com.hwlee.erp.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 버퍼에 모인 감사 사건을 audit_log 에 기록한다.
 *
 * <p>{@code REQUIRES_NEW} — 비즈니스 트랜잭션 커밋 직후(afterCommit)에 새 트랜잭션으로 INSERT.
 * 변경자(changed_by)는 변경자 추적과 같은 소스({@link AuditorAware}=SecurityContext)에서 가져와
 * 층위 ①·② 가 같은 "누가" 를 공유하게 한다.
 *
 * <p>changesJson 은 이미 직렬화된 문자열로 들어오므로 여기서는 엔티티/프록시를 건드리지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogWriter {

    private final AuditLogRepository repository;
    private final AuditorAware<String> auditorAware;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(List<AuditEvent> events) {
        String changedBy = auditorAware.getCurrentAuditor().orElse("system");
        LocalDateTime now = LocalDateTime.now(clock);
        for (AuditEvent event : events) {
            repository.save(AuditLog.of(
                    event.entityType(),
                    event.entityId(),
                    event.action(),
                    changedBy,
                    now,
                    event.changesJson()));
        }
    }
}
