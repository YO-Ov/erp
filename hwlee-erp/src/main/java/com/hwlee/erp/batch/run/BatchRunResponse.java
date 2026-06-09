package com.hwlee.erp.batch.run;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.batch.core.JobExecution;

/**
 * 배치 실행 결과 응답 — 잡 이름/기준일/상태/시작·종료 시각.
 */
public record BatchRunResponse(
        String jobName,
        LocalDate closingDate,
        String status,
        String exitCode,
        LocalDateTime startTime,
        LocalDateTime endTime) {

    public static BatchRunResponse from(JobExecution execution, LocalDate closingDate) {
        return new BatchRunResponse(
                execution.getJobInstance().getJobName(),
                closingDate,
                execution.getStatus().name(),
                execution.getExitStatus().getExitCode(),
                execution.getStartTime(),
                execution.getEndTime());
    }
}
