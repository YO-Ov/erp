package com.hwlee.erp.audit;

import com.hwlee.erp.audit.dto.AuditLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 감사 로그 조회 — "누가 언제 어떤 데이터를 바꿨는가". ADMIN/FINANCE 만 열람.
 * 계획서 성공 기준("변경 이력을 조회할 수 있는 API 가 있다") 충족.
 */
@Tag(name = "감사 로그", description = "변경 이력 조회")
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
public class AuditLogController {

    private final AuditLogRepository repository;

    @Operation(summary = "감사 로그 조회", description = "entityType(+entityId) 로 변경 이력을 최신순 조회")
    @GetMapping
    public Page<AuditLogResponse> search(
            @RequestParam String entityType,
            @RequestParam(required = false) Long entityId,
            Pageable pageable) {
        Page<AuditLog> page = (entityId != null)
                ? repository.findByEntityTypeAndEntityIdOrderByChangedAtDesc(entityType, entityId, pageable)
                : repository.findByEntityTypeOrderByChangedAtDesc(entityType, pageable);
        return page.map(AuditLogResponse::from);
    }
}
