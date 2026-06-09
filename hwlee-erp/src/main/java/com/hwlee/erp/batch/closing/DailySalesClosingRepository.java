package com.hwlee.erp.batch.closing;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailySalesClosingRepository extends JpaRepository<DailySalesClosing, Long> {

    Optional<DailySalesClosing> findByClosingDate(LocalDate closingDate);

    List<DailySalesClosing> findByClosingDateBetweenOrderByClosingDateAsc(LocalDate from, LocalDate to);

    /** 멱등 재실행 — 같은 기준일 행을 비우고 다시 만든다. */
    @Modifying
    @Query("delete from DailySalesClosing c where c.closingDate = :closingDate")
    int deleteByClosingDate(@Param("closingDate") LocalDate closingDate);
}
