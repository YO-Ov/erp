package com.hwlee.erp.pp.integration.mes;

import com.hwlee.erp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 처리 완료한 이벤트 기록 — Kafka 중복 수신을 막는 멱등 키 저장소.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "processed_event")
public class ProcessedEvent extends BaseEntity {

    @Column(name = "event_id", nullable = false, unique = true, length = 80)
    private String eventId;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public static ProcessedEvent of(String eventId, String source, LocalDateTime processedAt) {
        ProcessedEvent e = new ProcessedEvent();
        e.eventId = eventId;
        e.source = source;
        e.processedAt = processedAt;
        return e;
    }
}
