package com.hwlee.mes.workorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * ERP → MES 작업지시 수신 요청. {@code erpOrderNo} 가 멱등 키.
 */
public record WorkOrderReceiveRequest(
        @NotBlank String erpOrderNo,
        @NotBlank String productCode,
        @NotBlank String productName,
        @NotNull @Positive BigDecimal quantity,
        LocalDate plannedDate,
        @Valid List<ComponentLineRequest> components
) {
    public record ComponentLineRequest(
            @NotBlank String componentCode,
            @NotBlank String componentName,
            @NotNull @Positive BigDecimal requiredQty,
            String unit
    ) {}
}
