package com.hwlee.erp.sd.order.dto;

import java.math.BigDecimal;

public record SalesOrderLineResponse(
        Long id,
        int lineNo,
        Long itemId,
        String itemCode,
        String itemName,
        BigDecimal orderQty,
        BigDecimal shippedQty,
        BigDecimal invoicedQty,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
