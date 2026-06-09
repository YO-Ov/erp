package com.hwlee.erp.sd.order.dto;

import java.math.BigDecimal;

/**
 * 고객 신용한도 현황 — 수주 화면에서 확정 전에 미리 보여주기 위한 조회 응답.
 *
 * <ul>
 *   <li>{@code creditLimit}: 고객 마스터의 신용한도</li>
 *   <li>{@code used}: 한도에 영향을 주는 활성 수주(CONFIRMED~INVOICED)의 합계</li>
 *   <li>{@code remaining}: creditLimit - used (남은 한도)</li>
 * </ul>
 */
public record CreditStatusResponse(
        Long customerId,
        BigDecimal creditLimit,
        BigDecimal used,
        BigDecimal remaining
) {}
