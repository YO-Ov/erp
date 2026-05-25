package com.hwlee.erp.sd.invoice.dto;

import java.math.BigDecimal;

public record InvoiceLineResponse(
        Long id,
        int lineNo,
        Long salesOrderLineId,
        Long itemId,
        String itemCode,
        String itemName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
