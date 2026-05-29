package com.hwlee.erp.fi.journal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalEntryRepository
        extends JpaRepository<JournalEntry, Long>, JpaSpecificationExecutor<JournalEntry> {

    Optional<JournalEntry> findByNumber(String number);

    /** 출처별 조회 — 시연/디버깅 ("이 인보이스로 만든 전표를 보여줘"). */
    List<JournalEntry> findBySourceTypeAndSourceId(JournalSource sourceType, Long sourceId);

    /**
     * 라인과 계정까지 fetch join — 트랜잭션 밖에서도 라인을 순회할 수 있게 한다.
     * 테스트/조회 API 등 응답 직렬화가 트랜잭션 밖에서 일어나는 경우 LazyInitializationException 방지용.
     */
    @Query("SELECT DISTINCT je FROM JournalEntry je "
            + "LEFT JOIN FETCH je.lines l "
            + "LEFT JOIN FETCH l.account "
            + "WHERE je.sourceType = :sourceType AND je.sourceId = :sourceId "
            + "ORDER BY je.id ASC")
    List<JournalEntry> findBySourceTypeAndSourceIdWithLines(
            @Param("sourceType") JournalSource sourceType,
            @Param("sourceId") Long sourceId);

    /** 계정별 라인 조회 — 잔액(SUM(debit-credit)) 계산용. */
    @Query("SELECT l FROM JournalLine l "
            + "JOIN FETCH l.account a "
            + "WHERE a.code = :accountCode "
            + "AND l.journalEntry.status = com.hwlee.erp.fi.journal.JournalEntryStatus.POSTED")
    List<JournalLine> findPostedLinesByAccountCode(@Param("accountCode") String accountCode);
}
