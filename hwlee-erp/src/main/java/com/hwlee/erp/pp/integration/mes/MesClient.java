package com.hwlee.erp.pp.integration.mes;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * MES 호출 클라이언트 — ERP → MES 작업지시 전송(동기 REST).
 *
 * <p>회복성 3종:
 * <ul>
 *   <li><b>Timeout</b>: connect/read 타임아웃으로 무한 대기 방지.</li>
 *   <li><b>Retry</b>: 일시 실패는 몇 번 재시도(멱등 API 라 안전).</li>
 *   <li><b>Circuit Breaker</b>: 연속 실패가 임계치를 넘으면 회로를 열어, 한동안 즉시 fallback 으로
 *       빠르게 실패시켜 ERP 스레드 고갈(장애 전파)을 막는다.</li>
 * </ul>
 */
@Slf4j
@Component
public class MesClient {

    private final RestClient restClient;

    public MesClient(@Value("${mes.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @CircuitBreaker(name = "mes", fallbackMethod = "sendFallback")
    @Retry(name = "mes")
    public MesWorkOrderResponse sendWorkOrder(MesWorkOrderRequest request) {
        log.info("[MES 전송] 작업지시 erpOrderNo={}", request.erpOrderNo());
        return restClient.post()
                .uri("/api/work-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(MesWorkOrderResponse.class);
    }

    /** 재시도·서킷브레이커 후에도 실패 시 호출되는 fallback — 빠른 실패. */
    @SuppressWarnings("unused")
    private MesWorkOrderResponse sendFallback(MesWorkOrderRequest request, Throwable t) {
        log.warn("[MES 전송 실패] erpOrderNo={} cause={}", request.erpOrderNo(), t.toString());
        throw new MesUnavailableException(
                "MES 작업지시 전송 실패 (" + request.erpOrderNo() + "): " + t.getMessage(), t);
    }

    /** 정합성 검증용 — MES 작업지시 전체 조회. */
    @CircuitBreaker(name = "mes", fallbackMethod = "fetchFallback")
    @Retry(name = "mes")
    public List<MesWorkOrderSummary> fetchWorkOrders() {
        return restClient.get()
                .uri("/api/work-orders")
                .retrieve()
                .body(new ParameterizedTypeReference<List<MesWorkOrderSummary>>() {});
    }

    @SuppressWarnings("unused")
    private List<MesWorkOrderSummary> fetchFallback(Throwable t) {
        log.warn("[MES 조회 실패] cause={}", t.toString());
        throw new MesUnavailableException("MES 작업지시 조회 실패: " + t.getMessage(), t);
    }
}
