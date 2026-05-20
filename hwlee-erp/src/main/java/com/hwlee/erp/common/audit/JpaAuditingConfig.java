package com.hwlee.erp.common.audit;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화 + 현재 사용자 제공자(AuditorAware) 등록.
 *
 * <p>Phase 1 에서는 인증 시스템이 없으므로 항상 "system" 을 반환한다.
 * Phase 6 (Spring Security 도입) 에서 SecurityContext 기반으로 교체된다.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    static final String SYSTEM_USER = "system";

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(SYSTEM_USER);
    }
}
