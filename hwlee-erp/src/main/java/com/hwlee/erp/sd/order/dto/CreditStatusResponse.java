package com.hwlee.erp.sd.order.dto;

import java.math.BigDecimal;

/**
 * 고객 신용한도 현황 — 수주 화면에서 확정 전에 미리 보여주기 위한 조회 응답.
 *
 * <ul>
 *   <li>{@code creditLimit}: 고객 마스터의 여신한도</li>
 *   <li>{@code used}: 여신사용액 = {@code orderBacklog + receivable}</li>
 *   <li>{@code orderBacklog}: ① 미청구 활성수주(CONFIRMED~SHIPPED) 합</li>
 *   <li>{@code receivable}: ② 미수금(발행 인보이스 − 입금, 하한 0)</li>
 *   <li>{@code remaining}: creditLimit - used (가용한도)</li>
 * </ul>
 *
 * <p>입금이 들어오면 {@code receivable} 이 줄어 {@code used} 가 감소, {@code remaining} 이 회복된다.
 */
public record CreditStatusResponse(
        Long customerId,
        BigDecimal creditLimit,
        BigDecimal used,
        BigDecimal orderBacklog,
        BigDecimal receivable,
        BigDecimal remaining
) {}
