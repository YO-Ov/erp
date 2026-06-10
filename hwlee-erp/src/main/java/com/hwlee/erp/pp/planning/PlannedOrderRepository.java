package com.hwlee.erp.pp.planning;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PlannedOrderRepository
        extends JpaRepository<PlannedOrder, Long>, JpaSpecificationExecutor<PlannedOrder> {

    /** 전환으로 만든 생산지시 번호로 역조회 — 생산지시 취소 시 계획오더를 되살리기 위함. */
    Optional<PlannedOrder> findByConvertedProductionNumber(String convertedProductionNumber);
}
