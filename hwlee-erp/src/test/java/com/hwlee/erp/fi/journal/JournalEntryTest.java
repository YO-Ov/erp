package com.hwlee.erp.fi.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hwlee.erp.fi.account.Account;
import com.hwlee.erp.fi.account.AccountType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 회계 전표 도메인 — 복식부기의 핵심 불변식 검증.
 *
 * <p>가장 중요한 한 가지: <b>차변 합 = 대변 합</b> 이 아니면 {@link UnbalancedJournalException}.
 * 이 한 줄이 Phase 5 의 "회계 모듈이 지키는 무결성" 의 본체다.
 */
class JournalEntryTest {

    private final LocalDate today = LocalDate.now();
    private final LocalDateTime now = LocalDateTime.now();

    private final Account ar    = Account.create("1200", "매출채권",     AccountType.ASSET,     null, true);
    private final Account sales = Account.create("4100", "매출",          AccountType.REVENUE,   null, true);
    private final Account vat   = Account.create("2200", "부가세예수금", AccountType.LIABILITY, null, true);
    private final Account asset = Account.create("1000", "자산",          AccountType.ASSET,     null, false); // 헤더

    @Test
    @DisplayName("차변 합 = 대변 합 — POSTED 로 전이")
    void 차변_대변_일치_시_POSTED() {
        JournalEntry je = JournalEntry.draft(
                "JE-20260524-001", today, "매출 INV-001", JournalSource.INV, 1L);
        je.addDebit(ar,    new BigDecimal("13200000"));
        je.addCredit(sales, new BigDecimal("12000000"));
        je.addCredit(vat,   new BigDecimal("1200000"));

        je.post(now);

        assertThat(je.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(je.getPostedAt()).isEqualTo(now);
        assertThat(je.getTotalDebit()).isEqualByComparingTo("13200000");
        assertThat(je.getTotalCredit()).isEqualByComparingTo("13200000");
    }

    @Test
    @DisplayName("차변 합 ≠ 대변 합 → UnbalancedJournalException, 상태는 DRAFT 유지")
    void 차변_대변_불일치_시_예외() {
        JournalEntry je = JournalEntry.draft(
                "JE-20260524-002", today, "잘못된 분개", JournalSource.MANUAL, null);
        je.addDebit(ar,    new BigDecimal("1000"));
        je.addCredit(sales, new BigDecimal("999"));   // 1원 불일치

        assertThatThrownBy(() -> je.post(now))
                .isInstanceOf(UnbalancedJournalException.class)
                .hasMessageContaining("1000")
                .hasMessageContaining("999");

        assertThat(je.getStatus())
                .as("post 실패 시 상태는 DRAFT 그대로 — 트랜잭션 롤백 없이도 도메인이 안전")
                .isEqualTo(JournalEntryStatus.DRAFT);
    }

    @Test
    @DisplayName("라인이 없으면 post 불가")
    void 라인_없는_전표는_post_불가() {
        JournalEntry je = JournalEntry.draft(
                "JE-20260524-003", today, "빈 전표", JournalSource.MANUAL, null);

        assertThatThrownBy(() -> je.post(now))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("라인");
    }

    @Test
    @DisplayName("헤더 계정(postable=false)에는 라인 불가")
    void 헤더_계정에는_라인_불가() {
        JournalEntry je = JournalEntry.draft(
                "JE-20260524-004", today, "잘못된 라인", JournalSource.MANUAL, null);

        assertThatThrownBy(() -> je.addDebit(asset, new BigDecimal("1000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("postable=false");
    }

    @Test
    @DisplayName("한 라인은 차변 또는 대변 한 쪽만 양수 — 둘 다 양수면 거부")
    void 라인은_한쪽만_양수여야_한다() {
        JournalEntry je = JournalEntry.draft(
                "JE-20260524-005", today, "잘못된 라인", JournalSource.MANUAL, null);

        // JournalLine 생성자 직접 호출 — addDebit/addCredit 는 한 쪽 0 을 강제하므로 우회.
        assertThatThrownBy(() -> new JournalLine(
                je, 1, ar, new BigDecimal("100"), new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("차변 또는 대변 중 한 쪽");
    }

    @Test
    @DisplayName("POSTED 전표는 라인 추가 불가")
    void POSTED_전표는_라인_추가_불가() {
        JournalEntry je = JournalEntry.draft(
                "JE-20260524-006", today, "확정 후 추가 시도", JournalSource.MANUAL, null);
        je.addDebit(ar, new BigDecimal("1000"));
        je.addCredit(sales, new BigDecimal("1000"));
        je.post(now);

        assertThatThrownBy(() -> je.addDebit(ar, new BigDecimal("100")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    @DisplayName("POSTED → cancel — CANCELLED 로 전이")
    void POSTED_전표는_취소_가능() {
        JournalEntry je = JournalEntry.draft(
                "JE-20260524-007", today, "취소 대상", JournalSource.MANUAL, null);
        je.addDebit(ar, new BigDecimal("1000"));
        je.addCredit(sales, new BigDecimal("1000"));
        je.post(now);

        je.cancel();
        assertThat(je.getStatus()).isEqualTo(JournalEntryStatus.CANCELLED);
    }

    @Test
    @DisplayName("source=MANUAL 이 아니면 sourceId 필수")
    void INV_GI_GR_PAY_는_sourceId_필수() {
        assertThatThrownBy(() -> JournalEntry.draft(
                "JE-20260524-008", today, "출처 없는 매출", JournalSource.INV, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceId");
    }
}
