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
        BigDecimal lineTotal,
        /** 이 발주 라인의 품목에 대해 지금까지 입고된 누계(POSTED 입고 기준). */
        BigDecimal receivedQuantity,
        /** 미납 수량 = 발주수량 − 입고누계(하한 0). */
        BigDecimal openQuantity
) {}
