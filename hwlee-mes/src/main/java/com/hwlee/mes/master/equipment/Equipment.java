package com.hwlee.mes.master.equipment;

import com.hwlee.mes.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 설비(생산 라인/장비) 마스터. Phase 15 에서 가동 상태({@link EquipmentStatus})를 갖는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "equipment")
public class Equipment extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "line_name", length = 100)
    private String lineName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private EquipmentStatus status = EquipmentStatus.IDLE;

    public Equipment(String code, String name, String lineName) {
        this.code = code;
        this.name = name;
        this.lineName = lineName;
        this.status = EquipmentStatus.IDLE;
    }

    public void changeStatus(EquipmentStatus newStatus) {
        this.status = newStatus;
    }
}
