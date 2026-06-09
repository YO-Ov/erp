package com.hwlee.mes.master.equipment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentStatusLogRepository extends JpaRepository<EquipmentStatusLog, Long> {

    /** 현재 진행 중(미종료) 구간. */
    Optional<EquipmentStatusLog> findByEquipmentIdAndEndedAtIsNull(Long equipmentId);

    List<EquipmentStatusLog> findByEquipmentIdOrderByIdAsc(Long equipmentId);
}
