package com.hwlee.erp.mm.goodsissue.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record GoodsIssueLineRequest(
        @NotNull Long itemId,
        @NotNull @Positive BigDecimal quantity
) {}
