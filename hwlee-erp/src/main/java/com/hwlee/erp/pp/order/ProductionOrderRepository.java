package com.hwlee.erp.pp.order;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductionOrderRepository
        extends JpaRepository<ProductionOrder, Long>, JpaSpecificationExecutor<ProductionOrder> {

    /** 라인까지 fetch — 트랜잭션 밖 직렬화 시 LazyInitializationException 방지. */
    @Query("SELECT DISTINCT po FROM ProductionOrder po "
            + "LEFT JOIN FETCH po.lines l "
            + "LEFT JOIN FETCH l.component "
            + "WHERE po.id = :id")
    Optional<ProductionOrder> findByIdWithLines(@Param("id") Long id);

    /** Phase 14 — MES 실적 수신 시 생산지시번호(PO-...)로 조회. */
    Optional<ProductionOrder> findByNumber(String number);

    /** Phase 16 — 정합성 검증: MES 로 전송된(dispatched) 생산지시. */
    java.util.List<ProductionOrder> findByMesWorkOrderNoIsNotNull();
}
