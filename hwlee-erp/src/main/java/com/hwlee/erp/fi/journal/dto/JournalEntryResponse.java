package com.hwlee.erp.fi.journal.dto;

import com.hwlee.erp.fi.journal.JournalEntryStatus;
import com.hwlee.erp.fi.journal.JournalSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record JournalEntryResponse(
        Long id,
        String number,
        LocalDate entryDate,
        String description,
        JournalEntryStatus status,
        JournalSource sourceType,
        Long sourceId,
        LocalDateTime postedAt,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        List<JournalLineResponse> lines,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
