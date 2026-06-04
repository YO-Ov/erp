package com.hwlee.erp.pp.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 생산지시 생성 — 완제품·수량·창고. 소요 자재는 BOM×수량으로 자동 전개. */
public record ProductionOrderCreateRequest(
        @NotNull Long productItemId,
        @NotNull Long warehouseId,
        @NotNull @Positive BigDecimal quantity,
        @NotNull LocalDate orderDate,
        LocalDate dueDate
) {}
