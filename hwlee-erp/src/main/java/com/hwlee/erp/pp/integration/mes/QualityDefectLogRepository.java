package com.hwlee.erp.pp.integration.mes;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityDefectLogRepository extends JpaRepository<QualityDefectLog, Long> {

    boolean existsByEventId(String eventId);
}
