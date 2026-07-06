package com.hwlee.erp.mm.purchaseorder.dto;

import java.math.BigDecimal;

public record PurchaseOrderLineResponse(
        Long id,
        int lineNo,
        Long itemId,
        String itemCode,
        String itemName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
