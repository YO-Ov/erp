package com.hwlee.mes.integration.outbox;

import com.hwlee.mes.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Outbox 이벤트 — "보낼 메시지"를 DB 트랜잭션 안에 함께 적재한 것.
 * Publisher 가 PENDING 을 폴링해 Kafka 로 발행 후 SENT 로 표시한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "outbox_event")
public class OutboxEvent extends BaseEntity {

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 50)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_id", nullable = false, unique = true, length = 80)
    private String eventId;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    public static OutboxEvent pending(String aggregateType, String aggregateId,
                                      String eventType, String eventId, String payload) {
        OutboxEvent e = new OutboxEvent();
        e.aggregateType = aggregateType;
        e.aggregateId = aggregateId;
        e.eventType = eventType;
        e.eventId = eventId;
        e.payload = payload;
        e.status = OutboxStatus.PENDING;
        return e;
    }

    public void markSent(LocalDateTime now) {
        this.status = OutboxStatus.SENT;
        this.sentAt = now;
    }
}
