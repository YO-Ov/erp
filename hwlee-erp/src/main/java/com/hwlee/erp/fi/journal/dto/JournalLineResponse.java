package com.hwlee.erp.fi.journal.dto;

import java.math.BigDecimal;

public record JournalLineResponse(
        Long id,
        int lineNo,
        Long accountId,
        String accountCode,
        String accountName,
        BigDecimal debit,
        BigDecimal credit
) {}
