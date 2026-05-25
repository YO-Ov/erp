package com.hwlee.erp.sd.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record SalesOrderCreateRequest(
        @NotNull Long customerId,
        Long salespersonId,
        Long quotationId,
        @NotNull LocalDate orderDate,
        @NotEmpty @Valid List<SalesOrderLineRequest> lines
) {}
