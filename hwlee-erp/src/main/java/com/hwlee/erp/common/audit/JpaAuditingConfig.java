package com.hwlee.erp.common.audit;

import com.hwlee.erp.security.support.SecurityAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화 + 현재 사용자 제공자(AuditorAware) 등록.
 *
 * <p>Phase 1 에서는 인증 시스템이 없어 항상 "system" 을 반환했다.
 * Phase 6 (Spring Security 도입) 에서 {@link SecurityAuditorAware} 로 교체 — 복선 회수.
 * 미인증 컨텍스트(배치/시드/로그인 전)는 SecurityAuditorAware 내부에서 "system" 으로 fallback.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new SecurityAuditorAware();
    }
}
