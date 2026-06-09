package com.hwlee.erp.batch.run;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 야간/월초 자동 마감 스케줄러 — "실시간 처리와 배치의 책임 분리" 의 실물.
 *
 * <ul>
 *   <li>매일 02:30 — 전일분 일일 매출 마감.</li>
 *   <li>매월 1일 03:00 — 전월 말일 기준 월말 결산(재고 평가 + 채권 노령화).</li>
 * </ul>
 *
 * <p>수동 실행과 동일한 {@link ClosingBatchService} 를 호출하며, 멱등 설계 덕에 자동/수동이 충돌하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClosingBatchScheduler {

    private final ClosingBatchService service;

    /** 매일 새벽 02:30 — 전일 일일 매출 마감. */
    @Scheduled(cron = "0 30 2 * * *")
    public void nightlyDailySalesClosing() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("[스케줄] 일일 매출 마감 시작 date={}", yesterday);
        service.runDailySalesClosing(yesterday);
    }

    /** 매월 1일 새벽 03:00 — 전월 말일 기준 월말 결산. */
    @Scheduled(cron = "0 0 3 1 * *")
    public void monthlyMonthEndClosing() {
        LocalDate lastDayOfPrevMonth = LocalDate.now().withDayOfMonth(1).minusDays(1);
        log.info("[스케줄] 월말 결산 시작 date={}", lastDayOfPrevMonth);
        service.runMonthEndClosing(lastDayOfPrevMonth);
    }
}
