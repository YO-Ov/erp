package com.hwlee.erp.sd.delivery.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record DeliveryLineRequest(
        @NotNull Long salesOrderLineId,
        @NotNull @Positive BigDecimal quantity
) {}
