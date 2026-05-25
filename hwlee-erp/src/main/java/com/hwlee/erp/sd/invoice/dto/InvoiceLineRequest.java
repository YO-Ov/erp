package com.hwlee.erp.sd.invoice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record InvoiceLineRequest(
        @NotNull Long salesOrderLineId,
        @NotNull @Positive BigDecimal quantity
) {}
