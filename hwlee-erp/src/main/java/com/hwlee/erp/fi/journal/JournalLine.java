package com.hwlee.erp.fi.journal;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.fi.account.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전표 라인 — debit/credit 두 컬럼.
 *
 * <p>한 라인은 한 쪽만 > 0:
 * <ul>
 *   <li>차변 라인: {@code debit > 0, credit = 0}</li>
 *   <li>대변 라인: {@code debit = 0, credit > 0}</li>
 * </ul>
 * 둘 다 > 0 이거나 둘 다 0 은 금지 — 생성자와 DB CHECK 제약(V24)이 둘 다 강제.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "journal_line")
public class JournalLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "debit", nullable = false, precision = 15, scale = 2)
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(name = "credit", nullable = false, precision = 15, scale = 2)
    private BigDecimal credit = BigDecimal.ZERO;

    JournalLine(JournalEntry entry, int lineNo, Account account, BigDecimal debit, BigDecimal credit) {
        if (entry == null) throw new IllegalArgumentException("journalEntry 는 null 일 수 없다.");
        if (account == null) throw new IllegalArgumentException("account 는 null 일 수 없다.");
        if (!account.isPostable()) {
            throw new IllegalArgumentException(
                    "헤더 계정에는 라인을 달 수 없다 — postable=false: code=" + account.getCode());
        }
        if (debit == null || credit == null) {
            throw new IllegalArgumentException("debit/credit 은 null 일 수 없다.");
        }
        // 한 라인은 한 쪽만 > 0
        boolean debitPositive = debit.signum() > 0;
        boolean creditPositive = credit.signum() > 0;
        if (debitPositive == creditPositive) {
            throw new IllegalArgumentException(
                    "라인은 차변 또는 대변 중 한 쪽만 양수여야 한다. debit=" + debit + ", credit=" + credit);
        }
        if (debit.signum() < 0 || credit.signum() < 0) {
            throw new IllegalArgumentException("debit/credit 은 음수일 수 없다.");
        }
        this.journalEntry = entry;
        this.lineNo = lineNo;
        this.account = account;
        this.debit = debit;
        this.credit = credit;
    }
}
