package com.hwlee.erp.mm.goodsissue.dto;

import com.hwlee.erp.mm.goodsissue.GoodsIssueReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record GoodsIssueUpdateRequest(
        @NotNull Long warehouseId,
        @NotNull LocalDate issueDate,
        @NotNull GoodsIssueReason reason,
        @NotEmpty @Valid List<GoodsIssueLineRequest> lines
) {}
