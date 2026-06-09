package com.hwlee.mes.performance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** 생산 실적 보고 — 양품/불량 수량(부분 실적). */
public record ReportRequest(
        @NotNull @PositiveOrZero BigDecimal goodQty,
        @PositiveOrZero BigDecimal defectQty,
        String note) {
}
