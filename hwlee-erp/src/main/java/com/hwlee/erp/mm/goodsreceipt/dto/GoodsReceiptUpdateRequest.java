package com.hwlee.erp.mm.goodsreceipt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record GoodsReceiptUpdateRequest(
        @NotNull Long vendorId,
        @NotNull Long warehouseId,
        @NotNull LocalDate receiptDate,
        @NotEmpty @Valid List<GoodsReceiptLineRequest> lines
) {}
