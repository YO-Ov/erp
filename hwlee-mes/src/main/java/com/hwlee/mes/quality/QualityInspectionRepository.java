package com.hwlee.mes.quality;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityInspectionRepository extends JpaRepository<QualityInspection, Long> {

    List<QualityInspection> findByWorkOrderIdOrderByIdAsc(Long workOrderId);
}
