package com.hwlee.mes.quality.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** 품질 검사 등록. defectQty &gt; 0 이면 defectReasonId 필수. */
public record InspectRequest(
        @NotNull @PositiveOrZero BigDecimal inspectedQty,
        @NotNull @PositiveOrZero BigDecimal passedQty,
        @PositiveOrZero BigDecimal defectQty,
        Long defectReasonId,
        String note) {
}
