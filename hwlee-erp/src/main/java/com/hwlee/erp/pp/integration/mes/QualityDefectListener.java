package com.hwlee.erp.pp.integration.mes;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * MES 불량 통보 수신기 — 기록만 한다(재고/회계 영향 없음). event_id 로 멱등 처리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QualityDefectListener {

    private final QualityDefectLogRepository repository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "mes.quality.defect")
    @Transactional
    public void onMessage(String payload) {
        try {
            QualityDefectMessage msg = objectMapper.readValue(payload, QualityDefectMessage.class);
            if (repository.existsByEventId(msg.eventId())) {
                log.info("[MES 불량통보] 멱등 — 이미 기록됨 eventId={}", msg.eventId());
                return;
            }
            repository.save(QualityDefectLog.of(msg, LocalDateTime.now()));
            log.info("[MES 불량통보] 기록 eventId={} po={} 불량={} 사유={}",
                    msg.eventId(), msg.erpOrderNo(), msg.defectQty(), msg.defectReasonCode());
        } catch (Exception e) {
            log.error("[MES 불량통보] 처리 실패 payload={}", payload, e);
            throw new RuntimeException("불량 통보 처리 실패", e);
        }
    }
}
