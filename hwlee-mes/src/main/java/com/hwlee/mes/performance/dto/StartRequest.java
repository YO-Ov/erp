package com.hwlee.mes.performance.dto;

import jakarta.validation.constraints.NotNull;

/** 작업 시작 — 설비·작업자 배정. */
public record StartRequest(
        @NotNull Long equipmentId,
        @NotNull Long operatorId) {
}
