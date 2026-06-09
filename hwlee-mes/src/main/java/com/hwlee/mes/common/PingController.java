package com.hwlee.mes.common;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MES 생존 확인용 핑 — Phase 11 연계 토대 점검 및 ERP→MES RestClient 호출 대상.
 * 실제 작업지시 수신 API 는 Phase 12 에서 추가한다.
 */
@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "system", "hwlee-mes",
                "status", "UP",
                "time", LocalDateTime.now().toString());
    }
}
