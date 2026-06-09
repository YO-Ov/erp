package com.hwlee.erp.batch.job;

import com.hwlee.erp.batch.closing.DailySalesClosing;
import com.hwlee.erp.batch.closing.DailySalesClosingRepository;
import com.hwlee.erp.sd.invoice.Invoice;
import com.hwlee.erp.sd.invoice.InvoiceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 일일 매출 마감 잡 — 기준일의 ISSUED 인보이스를 합산해 {@link DailySalesClosing} 1행으로 박제.
 *
 * <p>단일 {@link Tasklet} 스텝. 한 트랜잭션 안에서 "기존 행 삭제 → 집계 → 저장" 을 수행하므로
 * 같은 날짜로 몇 번을 다시 돌려도 결과가 동일하다(멱등). 0건이어도 0원 마감 행을 남겨
 * "그 날 마감을 돌렸다" 는 사실을 기록한다.
 */
@Slf4j
@Configuration
public class DailySalesClosingJobConfig {

    @Bean
    public Job dailySalesClosingJob(JobRepository jobRepository, Step dailySalesClosingStep) {
        return new JobBuilder(BatchJobNames.DAILY_SALES_CLOSING, jobRepository)
                .start(dailySalesClosingStep)
                .build();
    }

    @Bean
    public Step dailySalesClosingStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      Tasklet dailySalesClosingTasklet) {
        return new StepBuilder("dailySalesClosingStep", jobRepository)
                .tasklet(dailySalesClosingTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet dailySalesClosingTasklet(
            @Value("#{jobParameters['" + BatchJobNames.PARAM_CLOSING_DATE + "']}") String closingDateStr,
            InvoiceRepository invoiceRepository,
            DailySalesClosingRepository closingRepository) {
        return (contribution, chunkContext) -> {
            LocalDate closingDate = LocalDate.parse(closingDateStr);

            // 멱등: 같은 날짜 기존 마감 제거.
            closingRepository.deleteByClosingDate(closingDate);

            List<Invoice> issued = invoiceRepository.findIssuedByInvoiceDate(closingDate);
            BigDecimal subtotal = sum(issued, Invoice::getSubtotal);
            BigDecimal taxAmount = sum(issued, Invoice::getTaxAmount);
            BigDecimal totalAmount = sum(issued, Invoice::getTotalAmount);

            closingRepository.save(DailySalesClosing.of(
                    closingDate, issued.size(), subtotal, taxAmount, totalAmount, LocalDateTime.now()));

            log.info("[일일매출마감] date={} count={} subtotal={} tax={} total={}",
                    closingDate, issued.size(), subtotal, taxAmount, totalAmount);
            contribution.incrementWriteCount(1);
            return RepeatStatus.FINISHED;
        };
    }

    private static BigDecimal sum(List<Invoice> invoices,
                                  java.util.function.Function<Invoice, BigDecimal> field) {
        return invoices.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
