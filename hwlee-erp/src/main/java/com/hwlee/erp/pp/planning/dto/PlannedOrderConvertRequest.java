package com.hwlee.erp.pp.planning.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 계획오더 → 생산지시 전환 요청. 담당자가 승인하면서 "어느 창고에 생산해 넣을지"(warehouseId)와
 * 납기(dueDate, 선택)를 지정한다 — 계획 단계엔 미정이던 정보를 여기서 굳힌다.
 */
public record PlannedOrderConvertRequest(
        @NotNull Long warehouseId,
        LocalDate dueDate
) {}
