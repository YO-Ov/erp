package com.hwlee.erp.fi.credit.dto;

/** 여신 요청 승인/거부 결정 (재무). 메모는 선택. */
public record CreditLimitDecisionRequest(
        String note
) {}
