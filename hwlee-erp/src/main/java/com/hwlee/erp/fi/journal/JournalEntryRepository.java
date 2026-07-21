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

    /** 재무 대시보드: 상태별 전표 건수 — 승인 대기(DRAFT) 카운트에 사용. */
    long countByStatus(JournalEntryStatus status);

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

    /**
     * Phase 10 — 손익계산서: 기간 내 POSTED 전표의 수익/비용 계정별 차·대 합.
     * 자산/부채/자본은 손익 대상이 아니므로 제외.
     */
    @Query("select new com.hwlee.erp.report.dto.AccountAmount("
            + "  a.code, a.name, a.type, sum(l.debit), sum(l.credit)) "
            + "from JournalLine l join l.account a join l.journalEntry je "
            + "where je.status = com.hwlee.erp.fi.journal.JournalEntryStatus.POSTED "
            + "and je.entryDate between :from and :to "
            + "and a.type in (com.hwlee.erp.fi.account.AccountType.REVENUE, "
            + "               com.hwlee.erp.fi.account.AccountType.EXPENSE) "
            + "group by a.code, a.name, a.type "
            + "order by a.code asc")
    List<com.hwlee.erp.report.dto.AccountAmount> incomeStatementSums(
            @Param("from") java.time.LocalDate from, @Param("to") java.time.LocalDate to);
}
