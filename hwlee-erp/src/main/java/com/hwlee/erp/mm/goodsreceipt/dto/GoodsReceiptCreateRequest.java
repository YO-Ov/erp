package com.hwlee.erp.mm.goodsreceipt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record GoodsReceiptCreateRequest(
        @NotNull Long vendorId,
        @NotNull Long warehouseId,
        @NotNull LocalDate receiptDate,
        /** 발주 참조(선택) — 구매발주로부터 입고 처리하는 경우 그 발주 id. 무발주 입고는 null. */
        Long purchaseOrderId,
        @NotEmpty @Valid List<GoodsReceiptLineRequest> lines
) {
    /** 발주 참조 없는 입고용 보조 생성자(무발주 입고·기존 호출부 호환). */
    public GoodsReceiptCreateRequest(Long vendorId, Long warehouseId, LocalDate receiptDate,
                                     List<GoodsReceiptLineRequest> lines) {
        this(vendorId, warehouseId, receiptDate, null, lines);
    }
}
