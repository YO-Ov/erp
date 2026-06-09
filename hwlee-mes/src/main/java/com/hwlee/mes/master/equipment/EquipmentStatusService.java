package com.hwlee.mes.master.equipment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 설비 상태 변경 + 가동률 계산.
 *
 * <p>상태를 바꿀 때 진행 중 구간을 닫고 새 구간을 연다. 가동률 = RUNNING 누적시간 / 전체 누적시간.
 * (OEE 의 가용성(Availability)에 해당하는 미니 버전.)
 */
@Service
@RequiredArgsConstructor
public class EquipmentStatusService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentStatusLogRepository logRepository;

    public record UtilizationResponse(
            String equipmentCode, EquipmentStatus currentStatus,
            long totalSeconds, long runningSeconds, BigDecimal utilizationPercent) {
    }

    @Transactional
    public EquipmentStatus changeStatus(Long equipmentId, EquipmentStatus newStatus) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("설비를 찾을 수 없습니다: " + equipmentId));
        LocalDateTime now = LocalDateTime.now();

        // 진행 중 구간 닫기.
        logRepository.findByEquipmentIdAndEndedAtIsNull(equipmentId)
                .ifPresent(open -> open.close(now));
        // 새 구간 시작 + 현재 상태 갱신.
        logRepository.save(EquipmentStatusLog.open(equipment, newStatus, now));
        equipment.changeStatus(newStatus);
        return newStatus;
    }

    @Transactional(readOnly = true)
    public UtilizationResponse utilization(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("설비를 찾을 수 없습니다: " + equipmentId));
        LocalDateTime now = LocalDateTime.now();

        long total = 0, running = 0;
        for (EquipmentStatusLog log : logRepository.findByEquipmentIdOrderByIdAsc(equipmentId)) {
            LocalDateTime end = (log.getEndedAt() != null) ? log.getEndedAt() : now;
            long sec = Math.max(0, Duration.between(log.getStartedAt(), end).getSeconds());
            total += sec;
            if (log.getStatus() == EquipmentStatus.RUNNING) {
                running += sec;
            }
        }
        BigDecimal pct = (total > 0)
                ? BigDecimal.valueOf(running).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new UtilizationResponse(equipment.getCode(), equipment.getStatus(), total, running, pct);
    }
}
