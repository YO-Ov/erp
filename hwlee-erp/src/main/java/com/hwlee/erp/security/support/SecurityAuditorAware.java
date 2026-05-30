package com.hwlee.erp.security.support;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * Phase 6 복선 회수 — JPA Auditing 의 created_by/updated_by 를 채울 현재 사용자 제공자.
 * 기존 "system" 고정({@code JpaAuditingConfig})을 SecurityContext 기반으로 교체한다.
 * 미인증 컨텍스트(배치/시드/로그인 전)는 "system" 으로 안전하게 fallback.
 * 이 빈 하나로 기존 SD/MM/FI/master 엔티티 코드는 0줄 수정한 채 변경자가 진짜 사용자로 바뀐다.
 */
public class SecurityAuditorAware implements AuditorAware<String> {

    static final String SYSTEM_USER = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of(SYSTEM_USER);
        }
        return Optional.of(authentication.getName());
    }
}
