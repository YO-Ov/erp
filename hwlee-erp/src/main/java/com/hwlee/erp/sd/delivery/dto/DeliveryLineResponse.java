package com.hwlee.erp.sd.delivery.dto;

import java.math.BigDecimal;

public record DeliveryLineResponse(
        Long id,
        int lineNo,
        Long salesOrderLineId,
        Long itemId,
        String itemCode,
        String itemName,
        BigDecimal quantity
) {}
