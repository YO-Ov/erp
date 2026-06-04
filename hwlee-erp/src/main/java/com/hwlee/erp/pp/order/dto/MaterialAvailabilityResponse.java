package com.hwlee.erp.pp.order.dto;

import java.math.BigDecimal;
import java.util.List;

/** 생산지시의 부품 가용성(소요량 vs 현재고). 막지 않는 참고용 조회. */
public record MaterialAvailabilityResponse(
        boolean producible,
        List<Line> lines
) {
    public record Line(
            Long componentItemId,
            String componentCode,
            String componentName,
            BigDecimal requiredQty,
            BigDecimal onHandQty,
            boolean sufficient
    ) {}
}
