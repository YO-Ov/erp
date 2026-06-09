package com.hwlee.mes.integration.outbox;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox Polling Publisher — PENDING 이벤트를 주기적으로 Kafka 로 발행하고 SENT 로 표시.
 *
 * <p>Kafka 발행 성공(브로커 ACK)을 확인한 뒤에만 SENT 처리한다. 실패한 건은 PENDING 으로 남아
 * 다음 주기에 재시도된다 → 최소 1회 전달(at-least-once). 중복은 수신측 멱등 처리로 흡수한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final int BATCH = 100;

    /** 이벤트 타입 → Kafka 토픽 매핑. (contracts/events/*) */
    private static String topicFor(String eventType) {
        return switch (eventType) {
            case "PRODUCTION_PERFORMANCE" -> "mes.production.performance";
            case "QUALITY_DEFECT" -> "mes.quality.defect";
            default -> throw new IllegalStateException("알 수 없는 이벤트 타입: " + eventType);
        };
    }

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxRepository.findByStatusOrderByIdAsc(
                OutboxStatus.PENDING, PageRequest.of(0, BATCH));
        if (pending.isEmpty()) {
            return;
        }
        for (OutboxEvent e : pending) {
            try {
                String topic = topicFor(e.getEventType());
                // .get() 으로 브로커 ACK 까지 동기 확인 후 SENT 처리.
                kafkaTemplate.send(topic, e.getAggregateId(), e.getPayload()).get();
                e.markSent(LocalDateTime.now());
                log.info("[Outbox 발행] eventId={} → topic={}", e.getEventId(), topic);
            } catch (Exception ex) {
                // 실패 → PENDING 유지(다음 주기 재시도). 루프 계속.
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.warn("[Outbox 발행 실패] eventId={} cause={}", e.getEventId(), ex.toString());
            }
        }
    }
}
