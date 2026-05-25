package com.hwlee.erp.sd.delivery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record DeliveryCreateRequest(
        @NotNull Long salesOrderId,
        @NotNull LocalDate shippedDate,
        @NotEmpty @Valid List<DeliveryLineRequest> lines
) {}
