package com.hwlee.erp.pp.integration.mes;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * MES 생산 실적 Kafka 수신기.
 *
 * <p>역직렬화 → {@link ProductionPerformanceHandler} 위임. 처리 실패 시 예외를 던져 Kafka 가 재전달하게
 * 한다(멱등 처리라 재시도가 안전). group-id=erp 는 application.yml 에서.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductionPerformanceListener {

    private final ProductionPerformanceHandler handler;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "mes.production.performance")
    public void onMessage(String payload) {
        try {
            ProductionPerformanceMessage msg =
                    objectMapper.readValue(payload, ProductionPerformanceMessage.class);
            handler.handle(msg);
        } catch (Exception e) {
            log.error("[MES 실적수신] 처리 실패 payload={}", payload, e);
            throw new RuntimeException("MES 실적 처리 실패", e);
        }
    }
}
