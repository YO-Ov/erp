package com.hwlee.erp.mm.goodsissue.dto;

import com.hwlee.erp.mm.goodsissue.GoodsIssueReason;
import com.hwlee.erp.mm.goodsissue.GoodsIssueStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GoodsIssueResponse(
        Long id,
        String number,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        GoodsIssueStatus status,
        LocalDate issueDate,
        GoodsIssueReason reason,
        LocalDateTime postedAt,
        List<GoodsIssueLineResponse> lines,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
