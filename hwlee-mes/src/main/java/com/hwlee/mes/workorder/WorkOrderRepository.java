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
}
