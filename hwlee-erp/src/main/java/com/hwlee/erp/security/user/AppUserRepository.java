package com.hwlee.erp.security.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);

    /** 특정 역할(code)을 가진 모든 사용자 — 알림 역할 fanout(예: FINANCE 전체)에 쓴다. */
    @Query("select distinct u from AppUser u join u.roles r where r.code = :roleCode and u.enabled = true")
    List<AppUser> findByRoleCode(@Param("roleCode") String roleCode);
}
