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
}
