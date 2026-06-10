package com.hwlee.erp.fi.credit.dto;

import com.hwlee.erp.fi.credit.CreditLimitRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreditLimitRequestResponse(
        Long id,
        String number,
        Long customerId,
        String customerName,
        BigDecimal currentLimit,
        BigDecimal requestedLimit,
        String reason,
        CreditLimitRequestStatus status,
        String requestedBy,
        LocalDateTime createdAt,
        String decidedBy,
        LocalDateTime decidedAt,
        String decisionNote
) {}
