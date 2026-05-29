package com.hwlee.erp.fi.journal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 수동 전표 생성 요청. 자동 분개는 이 API 를 거치지 않고
 * 이벤트 리스너 → {@link com.hwlee.erp.fi.journal.AutoJournalService} 가 직접 도메인 메서드로 생성한다.
 */
public record JournalEntryCreateRequest(
        @NotNull LocalDate entryDate,
        @NotBlank String description,
        @NotEmpty @Valid List<JournalLineRequest> lines
) {}
