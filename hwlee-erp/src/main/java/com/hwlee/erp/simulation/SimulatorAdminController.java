package com.hwlee.erp.simulation;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 데이터 시뮬레이터 수동 실행 — 관리자 전용.
 *
 * <p>정기 cron 틱을 기다리지 않고 즉시 한 배치(또는 여러 배치)를 생성하고 싶을 때 사용한다.
 * 시연/데모에서 "지금 바로 데이터를 채우고 싶다" 는 요구를 cron 을 건드리지 않고 해결한다.
 *
 * <p>시뮬레이터가 켜진 프로파일(운영)에서만 빈이 생성되므로({@link ConditionalOnProperty}),
 * 로컬처럼 시뮬레이터가 꺼진 환경에서는 이 엔드포인트 자체가 존재하지 않는다(404).
 */
@RestController
@RequestMapping("/api/admin/simulator")
@ConditionalOnProperty(name = "erp.simulator.enabled", havingValue = "true")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SimulatorAdminController {

    private final ErpDataSimulator simulator;

    /**
     * 즉시 데이터 배치 생성.
     *
     * @param batches 생성할 배치 수(기본 1, 1~10 로 제한). 각 배치는 cron 한 틱과 동일.
     * @return 배치 수와 총 생성 건수
     */
    @PostMapping("/tick")
    public Map<String, Object> tick(@RequestParam(defaultValue = "1") int batches) {
        int n = Math.max(1, Math.min(10, batches));
        int total = 0;
        for (int i = 0; i < n; i++) {
            total += simulator.generateBatch();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("batches", n);
        body.put("created", total);
        return body;
    }
}
