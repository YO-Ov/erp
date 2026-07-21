package com.hwlee.erp.sd.quotation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 기간별 견적 집계 응답 — "이번 달 견적 합계" 류 질의에 답하기 위한 것.
 *
 * <p>목록 API 를 클라이언트에서 합산하면 페이징 때문에 부정확하므로, 서버에서 정확히 집계해 내려준다.
 * ({@link com.hwlee.erp.sd.order.dto.SalesOrderSummaryResponse} 와 같은 이유·같은 모양.)
 *
 * <p>'견적 합계' 는 건수인지 금액인지 중의적이므로 둘 다 내려준다.
 * 기간의 기준일은 발행일({@code issuedDate}) 이다.
 */
public record QuotationSummaryResponse(
        LocalDate dateFrom,
        LocalDate dateTo,
        long quotationCount,   // 기간 내 견적 건수
        BigDecimal totalAmount // 기간 내 견적 금액 합계
) {}
