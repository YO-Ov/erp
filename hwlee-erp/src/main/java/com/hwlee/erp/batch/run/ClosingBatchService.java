package com.hwlee.erp.batch.run;

import com.hwlee.erp.batch.job.BatchJobNames;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 마감 배치 실행 진입점 — REST 컨트롤러와 스케줄러가 공유한다.
 *
 * <p>매 실행에 {@code run.id}(epoch millis)를 식별 파라미터로 추가해 항상 새 JobInstance 를 만든다.
 * 같은 기준일을 다시 돌려도 "이미 완료" 예외 없이 재집계되며, 결과의 멱등성은 스냅샷 테이블의
 * "삭제 후 재삽입" 으로 보장한다.
 */
@Slf4j
@Service
public class ClosingBatchService {

    private final JobLauncher jobLauncher;
    private final Job dailySalesClosingJob;
    private final Job monthEndClosingJob;

    public ClosingBatchService(JobLauncher jobLauncher,
                               @Qualifier(BatchJobNames.DAILY_SALES_CLOSING) Job dailySalesClosingJob,
                               @Qualifier(BatchJobNames.MONTH_END_CLOSING) Job monthEndClosingJob) {
        this.jobLauncher = jobLauncher;
        this.dailySalesClosingJob = dailySalesClosingJob;
        this.monthEndClosingJob = monthEndClosingJob;
    }

    /** 일일 매출 마감 실행(동기). */
    public JobExecution runDailySalesClosing(LocalDate closingDate) {
        return launch(dailySalesClosingJob, closingDate);
    }

    /** 월말 결산(재고 평가 + 채권 노령화) 실행(동기). */
    public JobExecution runMonthEndClosing(LocalDate closingDate) {
        return launch(monthEndClosingJob, closingDate);
    }

    private JobExecution launch(Job job, LocalDate closingDate) {
        JobParameters params = new JobParametersBuilder()
                .addString(BatchJobNames.PARAM_CLOSING_DATE, closingDate.toString())
                .addLong(BatchJobNames.PARAM_RUN_ID, System.currentTimeMillis())
                .toJobParameters();
        try {
            return jobLauncher.run(job, params);
        } catch (Exception e) {
            log.error("배치 실행 실패 job={} date={}", job.getName(), closingDate, e);
            throw new IllegalStateException(
                    "배치 실행 실패: " + job.getName() + " (" + closingDate + ") - " + e.getMessage(), e);
        }
    }
}
