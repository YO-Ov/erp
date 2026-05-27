package com.hwlee.erp.mm.stock;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository
        extends JpaRepository<Stock, Long>, JpaSpecificationExecutor<Stock> {

    Optional<Stock> findByItemIdAndWarehouseId(Long itemId, Long warehouseId);

    /**
     * 비관적 쓰기 락 (SELECT ... FOR UPDATE) 으로 (item, warehouse) 행을 점유한다.
     * 출고 경로({@code GoodsIssueService.post}) 의 핵심 — 동시 차감 race 를 원천 차단.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.item.id = :itemId and s.warehouse.id = :warehouseId")
    Optional<Stock> findForUpdate(@Param("itemId") Long itemId,
                                  @Param("warehouseId") Long warehouseId);
}
