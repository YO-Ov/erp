package com.hwlee.mes.master.equipment;

import com.hwlee.mes.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 설비 상태 변경 이력 — 한 상태로 머문 구간([startedAt, endedAt)). 가동률 계산의 원천.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "equipment_status_log")
public class EquipmentStatusLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private EquipmentStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public static EquipmentStatusLog open(Equipment equipment, EquipmentStatus status, LocalDateTime startedAt) {
        EquipmentStatusLog log = new EquipmentStatusLog();
        log.equipment = equipment;
        log.status = status;
        log.startedAt = startedAt;
        return log;
    }

    public void close(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }
}
