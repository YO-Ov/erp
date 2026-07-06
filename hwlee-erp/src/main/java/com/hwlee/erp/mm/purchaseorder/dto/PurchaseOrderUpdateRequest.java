package com.hwlee.erp.mm.purchaseorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderUpdateRequest(
        @NotNull Long vendorId,
        @NotNull Long warehouseId,
        @NotNull LocalDate orderDate,
        LocalDate expectedDate,
        @Size(max = 500) String remark,
        @NotEmpty @Valid List<PurchaseOrderLineRequest> lines
) {}
