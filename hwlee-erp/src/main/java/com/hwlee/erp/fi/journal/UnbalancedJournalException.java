package com.hwlee.erp.fi.journal;

import java.math.BigDecimal;
import lombok.Getter;

/**
 * 복식부기의 본질 — 차변 합과 대변 합이 일치하지 않을 때 던진다.
 *
 * <p>{@link JournalEntry#post} 가 검증해서 발생시킨다. Phase 3 의 {@code InsufficientStockException},
 * Phase 2 의 신용한도 예외와 같은 자리 — "도메인 불변식을 코드가 지킨다".
 *
 * <p>{@link com.hwlee.erp.common.error.GlobalExceptionHandler} 가 422 (UNPROCESSABLE_ENTITY) 로 변환한다.
 */
@Getter
public class UnbalancedJournalException extends RuntimeException {

    private final BigDecimal totalDebit;
    private final BigDecimal totalCredit;

    public UnbalancedJournalException(BigDecimal totalDebit, BigDecimal totalCredit) {
        super(String.format("전표 차/대 불일치 — 차변 합 %s, 대변 합 %s", totalDebit, totalCredit));
        this.totalDebit = totalDebit;
        this.totalCredit = totalCredit;
    }
}
