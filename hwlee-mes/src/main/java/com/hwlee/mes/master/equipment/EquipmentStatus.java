package com.hwlee.mes.master.equipment;

/** 설비 가동 상태. */
public enum EquipmentStatus {
    RUNNING,      // 가동
    IDLE,         // 대기
    DOWN,         // 고장/정지
    MAINTENANCE   // 정비
}
