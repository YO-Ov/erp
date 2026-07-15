package com.hwlee.mes.workorder;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    /** 멱등 수신의 핵심 — ERP PO 번호로 기존 작업지시를 찾는다. */
    Optional<WorkOrder> findByErpOrderNo(String erpOrderNo);

    @Query("select distinct w from WorkOrder w left join fetch w.lines order by w.id desc")
    List<WorkOrder> findAllWithLines();

    /**
     * 생산 시뮬레이터 대상 — 진행 중(IN_PROGRESS)이면서 배정 설비가 가동(RUNNING) 중인 작업지시.
     * "설비 가동을 켜면 그 설비의 작업지시가 생산된다"는 규칙의 질의.
     */
    @Query("select w from WorkOrder w join w.assignedEquipment e "
            + "where w.status = com.hwlee.mes.workorder.WorkOrderStatus.IN_PROGRESS "
            + "and e.status = com.hwlee.mes.master.equipment.EquipmentStatus.RUNNING")
    List<WorkOrder> findRunningInProgress();
}
