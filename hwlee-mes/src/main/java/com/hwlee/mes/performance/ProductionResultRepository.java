package com.hwlee.mes.performance;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductionResultRepository extends JpaRepository<ProductionResult, Long> {

    int countByWorkOrderId(Long workOrderId);

    @Query("select distinct r from ProductionResult r left join fetch r.consumptions "
            + "where r.workOrder.id = :workOrderId order by r.seq asc")
    List<ProductionResult> findByWorkOrderIdWithConsumptions(@Param("workOrderId") Long workOrderId);
}
