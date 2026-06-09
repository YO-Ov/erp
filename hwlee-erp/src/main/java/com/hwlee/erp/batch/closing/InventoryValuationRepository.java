package com.hwlee.erp.batch.closing;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryValuationRepository extends JpaRepository<InventoryValuation, Long> {

    List<InventoryValuation> findByValuationDateOrderByIdAsc(LocalDate valuationDate);

    /** 멱등 재실행 — 같은 평가일 행을 비우고 다시 만든다. */
    @Modifying
    @Query("delete from InventoryValuation v where v.valuationDate = :valuationDate")
    int deleteByValuationDate(@Param("valuationDate") LocalDate valuationDate);
}
