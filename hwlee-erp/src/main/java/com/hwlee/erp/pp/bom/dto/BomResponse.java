package com.hwlee.erp.pp.bom.dto;

import java.math.BigDecimal;

public record BomResponse(
        Long id,
        Long productItemId,
        String productCode,
        String productName,
        Long componentItemId,
        String componentCode,
        String componentName,
        BigDecimal quantity
) {}
