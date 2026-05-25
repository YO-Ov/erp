package com.hwlee.erp.sd.quotation.dto;

import java.math.BigDecimal;

public record QuotationLineResponse(
        Long id,
        int lineNo,
        Long itemId,
        String itemCode,
        String itemName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
