package com.hwlee.erp.fi.journal;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.fi.account.Account;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회계 전표 — 복식부기의 단위. 헤더-라인 구조 (Invoice/SO 와 같은 패턴).
 *
 * <p>핵심 불변식: <b>차변 합 = 대변 합</b> — {@link #post} 가 검증해서 어기면
 * {@link UnbalancedJournalException} 을 던진다. POSTED 이후로는 라인 수정 불가.
 *
 * <p>출처: {@link #sourceType} + {@link #sourceId} 약한 참조. FK 안 묶음(다형성).
 * MANUAL 이면 {@code sourceId = null} 이지만 sourceType 은 항상 채워진다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "journal_entry")
public class JournalEntry extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30, updatable = false)
    private String number;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private JournalEntryStatus status = JournalEntryStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 16)
    private JournalSource sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNo ASC")
    private List<JournalLine> lines = new ArrayList<>();

    public static JournalEntry draft(String number, LocalDate entryDate, String description,
                                     JournalSource sourceType, Long sourceId) {
        if (number == null || number.isBlank()) throw new IllegalArgumentException("number 는 비어 있을 수 없다.");
        if (entryDate == null) throw new IllegalArgumentException("entryDate 는 null 일 수 없다.");
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("description 은 비어 있을 수 없다.");
        if (sourceType == null) throw new IllegalArgumentException("sourceType 은 null 일 수 없다.");
        if (sourceType != JournalSource.MANUAL && sourceId == null) {
            throw new IllegalArgumentException("MANUAL 이 아닌 경우 sourceId 는 필수다.");
        }
        JournalEntry je = new JournalEntry();
        je.number = number;
        je.entryDate = entryDate;
        je.description = description;
        je.sourceType = sourceType;
        je.sourceId = sourceId;
        return je;
    }

    /** 차변 라인 추가 — {@code debit > 0}. */
    public JournalLine addDebit(Account account, BigDecimal amount) {
        return addLine(account, amount, BigDecimal.ZERO);
    }

    /** 대변 라인 추가 — {@code credit > 0}. */
    public JournalLine addCredit(Account account, BigDecimal amount) {
        return addLine(account, BigDecimal.ZERO, amount);
    }

    private JournalLine addLine(Account account, BigDecimal debit, BigDecimal credit) {
        if (status != JournalEntryStatus.DRAFT) {
            throw new IllegalStateException("DRAFT 전표만 라인 추가 가능합니다. 현재: " + status);
        }
        JournalLine line = new JournalLine(this, lines.size() + 1, account, debit, credit);
        lines.add(line);
        return line;
    }

    /**
     * 전표 확정 — 차변 합 = 대변 합 검증 후 POSTED.
     *
     * <p>불일치 시 {@link UnbalancedJournalException} — 복식부기의 핵심 불변식.
     */
    public void post(LocalDateTime now) {
        if (status != JournalEntryStatus.DRAFT) {
            throw new IllegalStateException("DRAFT 전표만 확정 가능합니다. 현재: " + status);
        }
        if (lines.isEmpty()) {
            throw new IllegalStateException("라인이 없는 전표는 확정할 수 없다.");
        }
        BigDecimal totalDebit = lines.stream()
                .map(JournalLine::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream()
                .map(JournalLine::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new UnbalancedJournalException(totalDebit, totalCredit);
        }
        this.status = JournalEntryStatus.POSTED;
        this.postedAt = now;
    }

    /**
     * 전표 취소 — 헤더 상태만 CANCELLED 로 표시.
     *
     * <p>회계 정석은 역분개(reverse entry) 를 별도 전표로 추가하는 것 — 원장은 append-only.
     * 헤더 상태도 같이 바꿔서 "이 전표는 취소된 사실" 을 드러내고, 원장 SUM 에서 제외할 때
     * 상태 필터로 한 번에 거를 수 있게 한다.
     */
    public void cancel() {
        if (status != JournalEntryStatus.POSTED) {
            throw new IllegalStateException("POSTED 전표만 취소 가능합니다. 현재: " + status);
        }
        this.status = JournalEntryStatus.CANCELLED;
    }

    public List<JournalLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public BigDecimal getTotalDebit() {
        return lines.stream().map(JournalLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalCredit() {
        return lines.stream().map(JournalLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
