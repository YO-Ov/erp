package com.hwlee.erp.mm.goodsreceipt.dto;

import com.hwlee.erp.mm.goodsreceipt.GoodsReceiptStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GoodsReceiptResponse(
        Long id,
        String number,
        Long vendorId,
        String vendorCode,
        String vendorName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        GoodsReceiptStatus status,
        LocalDate receiptDate,
        LocalDateTime postedAt,
        List<GoodsReceiptLineResponse> lines,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
