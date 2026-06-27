package com.hwlee.erp.master.item.dto;

import com.hwlee.erp.master.item.ItemUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ItemCreateRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 20) String category,
        @NotNull ItemUnit unit,
        @NotNull @PositiveOrZero BigDecimal standardCost,
        @NotNull @PositiveOrZero BigDecimal standardPrice
) {}
