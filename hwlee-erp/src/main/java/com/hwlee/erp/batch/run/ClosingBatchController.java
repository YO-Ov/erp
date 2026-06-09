package com.hwlee.erp.batch.run;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마감 배치 수동 실행 REST — 야간 스케줄과 별개로 운영자가 특정 기준일을 재집계할 때 사용.
 *
 * <p>{@code date} 생략 시 합리적 기본값으로 동작한다(일일=어제, 월말=전월 말일).
 */
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
public class ClosingBatchController {

    private final ClosingBatchService service;

    /** 일일 매출 마감. date 생략 시 어제. */
    @PostMapping("/daily-sales-closing")
    public BatchRunResponse runDailySalesClosing(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate closingDate = (date != null) ? date : LocalDate.now().minusDays(1);
        JobExecution execution = service.runDailySalesClosing(closingDate);
        return BatchRunResponse.from(execution, closingDate);
    }

    /** 월말 결산(재고 평가 + 채권 노령화). date 생략 시 전월 말일. */
    @PostMapping("/month-end-closing")
    public BatchRunResponse runMonthEndClosing(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate closingDate = (date != null) ? date : LocalDate.now().withDayOfMonth(1).minusDays(1);
        JobExecution execution = service.runMonthEndClosing(closingDate);
        return BatchRunResponse.from(execution, closingDate);
    }
}
