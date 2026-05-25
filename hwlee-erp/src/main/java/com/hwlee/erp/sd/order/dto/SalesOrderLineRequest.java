package com.hwlee.erp.sd.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record SalesOrderLineRequest(
        @NotNull Long itemId,
        @NotNull @Positive BigDecimal orderQty,
        @NotNull @PositiveOrZero BigDecimal unitPrice
) {}
