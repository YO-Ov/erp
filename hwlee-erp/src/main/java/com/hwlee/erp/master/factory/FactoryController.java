package com.hwlee.erp.master.factory;

import com.hwlee.erp.common.entity.MasterStatus;
import com.hwlee.erp.master.factory.dto.FactoryResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공장 마스터 조회 API — 창고/생산 화면의 공장 드롭다운·표시에 사용.
 * (공장은 자주 바뀌지 않으므로 생성/수정은 우선 시드/마이그레이션으로 관리한다.)
 */
@RestController
@RequestMapping("/api/factories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES','PURCHASING','FINANCE','PRODUCTION','ADMIN')")
public class FactoryController {

    private final FactoryRepository repository;

    @GetMapping
    public List<FactoryResponse> list() {
        return repository.findAllByStatusOrderByCodeAsc(MasterStatus.ACTIVE).stream()
                .map(FactoryController::toResponse)
                .toList();
    }

    @GetMapping("/by-code/{code}")
    public FactoryResponse findByCode(@PathVariable String code) {
        return repository.findByCode(code)
                .map(FactoryController::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Factory not found: code=" + code));
    }

    private static FactoryResponse toResponse(Factory f) {
        return new FactoryResponse(f.getId(), f.getCode(), f.getName(), f.getAddress(), f.getStatus());
    }
}
