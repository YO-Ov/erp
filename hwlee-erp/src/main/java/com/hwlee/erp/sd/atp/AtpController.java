package com.hwlee.erp.sd.atp;

import com.hwlee.erp.sd.atp.dto.AtpResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 약속가능재고(ATP) 조회 — 영업이 수주를 약속하기 전에 품목별 가용 수량을 확인한다.
 * 재고는 "조회는 넓게, 변경(입고·출고)은 PURCHASING 만" 원칙에 따라 조회 권한을 영업까지 넓힌다.
 */
@RestController
@RequestMapping("/api/atp")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES','PURCHASING','FINANCE','ADMIN')")
public class AtpController {

    private final AtpService service;

    @GetMapping
    public AtpResponse atp(@RequestParam Long itemId) {
        return service.atp(itemId);
    }
}
