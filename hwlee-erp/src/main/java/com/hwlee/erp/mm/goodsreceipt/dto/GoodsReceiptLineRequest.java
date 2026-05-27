package com.hwlee.erp.mm.goodsreceipt.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record GoodsReceiptLineRequest(
        @NotNull Long itemId,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @PositiveOrZero BigDecimal unitCost
) {}
