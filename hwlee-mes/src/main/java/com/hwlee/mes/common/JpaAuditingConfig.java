package com.hwlee.mes.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** JPA Auditing(created_at/updated_at 자동 기록) 활성화. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
