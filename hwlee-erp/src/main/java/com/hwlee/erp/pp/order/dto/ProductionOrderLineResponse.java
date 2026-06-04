package com.hwlee.erp.pp.order.dto;

import java.math.BigDecimal;

public record ProductionOrderLineResponse(
        Long id,
        int lineNo,
        Long componentItemId,
        String componentCode,
        String componentName,
        BigDecimal requiredQty,
        BigDecimal issuedUnitCost
) {}
