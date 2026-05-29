package com.hwlee.erp.fi.account;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 계정 유형 5종 — 회계의 5대 분류. 정상 잔액 방향({@link NormalSide})을 함께 가진다.
 *
 * <ul>
 *   <li>{@link #ASSET} 자산 — 차변 정상(현금/매출채권/재고)</li>
 *   <li>{@link #LIABILITY} 부채 — 대변 정상(매입채무/부가세예수금)</li>
 *   <li>{@link #EQUITY} 자본 — 대변 정상(자본금/이익잉여금)</li>
 *   <li>{@link #REVENUE} 수익 — 대변 정상(매출/잡수입)</li>
 *   <li>{@link #EXPENSE} 비용 — 차변 정상(매출원가/판관비)</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum AccountType {
    ASSET(NormalSide.DEBIT),
    LIABILITY(NormalSide.CREDIT),
    EQUITY(NormalSide.CREDIT),
    REVENUE(NormalSide.CREDIT),
    EXPENSE(NormalSide.DEBIT);

    private final NormalSide normalSide;
}
