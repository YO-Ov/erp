package com.hwlee.erp.master.vendoritem.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record VendorItemCreateRequest(
        @NotNull Long vendorId,
        @NotNull Long itemId,
        @NotNull @PositiveOrZero BigDecimal supplyPrice,
        @PositiveOrZero int leadTimeDays
) {}
