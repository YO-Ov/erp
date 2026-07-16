package com.hwlee.mes.master.equipment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 설비 상태 변경 + 가동률(시간가동률) 계산.
 *
 * <p>상태를 바꿀 때 진행 중 구간을 닫고 새 구간을 연다. 가동률은 실무 OEE 의
 * 시간가동률(Availability)을 따른다:
 * <pre>
 *   시간가동률 = 가동시간(RUNNING) / 부하시간
 *   부하시간   = 돌리기로 계획된 시간 = RUNNING + DOWN(고장)
 *              (대기 IDLE·정비 MAINTENANCE 는 계획정지 → 분모에서 제외)
 * </pre>
 * <p>집계 범위는 <b>오늘(당일 00:00 ~ 현재)</b> 이다. 전체 누적이 아니라 하루 단위라,
 * 초기 표본이 적을 때의 100%/0% 극단값과 상태 토글에 따른 널뛰기가 완화된다.
 * 오늘 부하시간이 0 이면(가동·고장이 아직 없었으면) 가동률은 null("집계 전")이다.
 */
@Service
@RequiredArgsConstructor
public class EquipmentStatusService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentStatusLogRepository logRepository;

    /** utilizationPercent 는 오늘 부하시간이 0 이면 null(집계 전). */
    public record UtilizationResponse(
            String equipmentCode, EquipmentStatus currentStatus,
            long loadingSeconds, long operatingSeconds, BigDecimal utilizationPercent) {
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
        LocalDateTime dayStart = now.toLocalDate().atStartOfDay(); // 오늘 00:00 — 집계 시작점

        long loading = 0, operating = 0;
        for (EquipmentStatusLog log : logRepository.findByEquipmentIdOrderByIdAsc(equipmentId)) {
            LocalDateTime rawEnd = (log.getEndedAt() != null) ? log.getEndedAt() : now;
            // 오늘 구간만 계산: 시작이 어제 이전이면 오늘 0시로 클립하고, 오늘 이전에 끝난 구간은 건너뛴다.
            LocalDateTime start = log.getStartedAt().isBefore(dayStart) ? dayStart : log.getStartedAt();
            if (!rawEnd.isAfter(start)) {
                continue;
            }
            long sec = Math.max(0, Duration.between(start, rawEnd).getSeconds());
            if (log.getStatus().countsAsLoading()) {   // 부하시간(RUNNING + DOWN)만 분모에
                loading += sec;
                if (log.getStatus().isOperating()) {   // 그중 실제 가동(RUNNING)만 분자에
                    operating += sec;
                }
            }
        }
        // 오늘 부하시간이 0(가동·고장이 아직 없음)이면 가동률은 정의되지 않음 → null("집계 전").
        BigDecimal pct = (loading > 0)
                ? BigDecimal.valueOf(operating).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(loading), 1, RoundingMode.HALF_UP)
                : null;
        return new UtilizationResponse(equipment.getCode(), equipment.getStatus(), loading, operating, pct);
    }
}
