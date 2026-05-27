package com.hwlee.erp.mm.goodsreceipt.dto;

import java.math.BigDecimal;

public record GoodsReceiptLineResponse(
        Long id,
        int lineNo,
        Long itemId,
        String itemCode,
        String itemName,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal lineTotal
) {}
