package com.hwlee.erp.mm.goodsissue.dto;

import java.math.BigDecimal;

public record GoodsIssueLineResponse(
        Long id,
        int lineNo,
        Long itemId,
        String itemCode,
        String itemName,
        BigDecimal quantity
) {}
