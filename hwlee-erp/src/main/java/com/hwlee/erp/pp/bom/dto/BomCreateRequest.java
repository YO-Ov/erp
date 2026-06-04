package com.hwlee.erp.pp.bom.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** BOM 한 줄 등록 — 완제품 1개당 부품 소요량. */
public record BomCreateRequest(
        @NotNull Long productItemId,
        @NotNull Long componentItemId,
        @NotNull @Positive BigDecimal quantity
) {}
