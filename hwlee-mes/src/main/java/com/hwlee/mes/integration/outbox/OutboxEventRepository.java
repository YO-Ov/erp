package com.hwlee.mes.integration.outbox;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /** 미발행 이벤트를 오래된 순으로 조회(배치 발행용). */
    List<OutboxEvent> findByStatusOrderByIdAsc(OutboxStatus status, Pageable pageable);
}
