package com.hwlee.erp.batch.job;

import com.hwlee.erp.batch.closing.ArAging;
import com.hwlee.erp.batch.closing.ArAgingBuckets;
import com.hwlee.erp.batch.closing.ArAgingRepository;
import com.hwlee.erp.batch.closing.InventoryValuation;
import com.hwlee.erp.batch.closing.InventoryValuationRepository;
import com.hwlee.erp.master.customer.Customer;
import com.hwlee.erp.mm.stock.Stock;
import com.hwlee.erp.fi.payment.Payment;
import com.hwlee.erp.fi.payment.PaymentRepository;
import com.hwlee.erp.sd.invoice.Invoice;
import com.hwlee.erp.sd.invoice.InvoiceRepository;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 월말 결산 잡 — 세 스텝을 순차 실행.
 *
 * <ol>
 *   <li><b>purgeStep</b>(Tasklet): 기준일의 재고평가·채권노령화 스냅샷을 비운다(멱등 재실행).</li>
 *   <li><b>inventoryValuationStep</b>(Chunk): 모든 {@link Stock} 을 읽어 평가액을 계산해
 *       {@link InventoryValuation} 으로 저장한다.</li>
 *   <li><b>arAgingStep</b>(Tasklet): 고객별 미수금을 FIFO 로 경과일 버킷팅해 {@link ArAging} 으로 저장한다.</li>
 * </ol>
 *
 * <p>chunk 스텝이 중간에 깨져 일부만 커밋되더라도, 다음 실행의 purgeStep 이 다시 비우므로 안전하다.
 */
@Slf4j
@Configuration
public class MonthEndClosingJobConfig {

    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job monthEndClosingJob(JobRepository jobRepository,
                                  Step monthEndPurgeStep,
                                  Step inventoryValuationStep,
                                  Step arAgingStep) {
        return new JobBuilder(BatchJobNames.MONTH_END_CLOSING, jobRepository)
                .start(monthEndPurgeStep)
                .next(inventoryValuationStep)
                .next(arAgingStep)
                .build();
    }

    // ---------- Step 1: 멱등 purge ----------

    @Bean
    public Step monthEndPurgeStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  Tasklet monthEndPurgeTasklet) {
        return new StepBuilder("monthEndPurgeStep", jobRepository)
                .tasklet(monthEndPurgeTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet monthEndPurgeTasklet(
            @Value("#{jobParameters['" + BatchJobNames.PARAM_CLOSING_DATE + "']}") String closingDateStr,
            InventoryValuationRepository valuationRepository,
            ArAgingRepository arAgingRepository) {
        return (contribution, chunkContext) -> {
            LocalDate date = LocalDate.parse(closingDateStr);
            int v = valuationRepository.deleteByValuationDate(date);
            int a = arAgingRepository.deleteByAgingDate(date);
            log.info("[월말결산:purge] date={} 재고평가삭제={} 채권노령화삭제={}", date, v, a);
            return RepeatStatus.FINISHED;
        };
    }

    // ---------- Step 2: 재고 평가 (chunk) ----------

    @Bean
    public Step inventoryValuationStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager,
                                       JpaPagingItemReader<Stock> stockItemReader,
                                       ItemProcessor<Stock, InventoryValuation> stockValuationProcessor,
                                       JpaItemWriter<InventoryValuation> inventoryValuationWriter) {
        return new StepBuilder("inventoryValuationStep", jobRepository)
                .<Stock, InventoryValuation>chunk(CHUNK_SIZE, transactionManager)
                .reader(stockItemReader)
                .processor(stockValuationProcessor)
                .writer(inventoryValuationWriter)
                .build();
    }

    @Bean
    public JpaPagingItemReader<Stock> stockItemReader(EntityManagerFactory entityManagerFactory) {
        return new JpaPagingItemReaderBuilder<Stock>()
                .name("stockItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("select s from Stock s "
                        + "join fetch s.item join fetch s.warehouse order by s.id asc")
                .pageSize(CHUNK_SIZE)
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<Stock, InventoryValuation> stockValuationProcessor(
            @Value("#{jobParameters['" + BatchJobNames.PARAM_CLOSING_DATE + "']}") String closingDateStr) {
        LocalDate valuationDate = LocalDate.parse(closingDateStr);
        return stock -> {
            // 보유 0 인 재고는 평가 대상 제외(null 반환 = 필터).
            if (stock.getQtyOnHand().signum() == 0) {
                return null;
            }
            return InventoryValuation.of(valuationDate, stock.getItem(), stock.getWarehouse(),
                    stock.getQtyOnHand(), stock.getAverageCost());
        };
    }

    @Bean
    public JpaItemWriter<InventoryValuation> inventoryValuationWriter(
            EntityManagerFactory entityManagerFactory) {
        return new JpaItemWriterBuilder<InventoryValuation>()
                .entityManagerFactory(entityManagerFactory)
                .usePersist(true) // 신규 엔티티 — merge 대신 persist.
                .build();
    }

    // ---------- Step 3: 채권 노령화 (tasklet) ----------

    @Bean
    public Step arAgingStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager,
                            Tasklet arAgingTasklet) {
        return new StepBuilder("arAgingStep", jobRepository)
                .tasklet(arAgingTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet arAgingTasklet(
            @Value("#{jobParameters['" + BatchJobNames.PARAM_CLOSING_DATE + "']}") String closingDateStr,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            ArAgingRepository arAgingRepository) {
        return (contribution, chunkContext) -> {
            LocalDate agingDate = LocalDate.parse(closingDateStr);

            List<Invoice> invoices = invoiceRepository.findIssuedUpToWithCustomer(agingDate);
            List<Payment> receipts = paymentRepository.findPostedReceiptsUpTo(agingDate);

            // 고객별 누적 입금액.
            Map<Long, BigDecimal> paidByCustomer = receipts.stream()
                    .collect(Collectors.groupingBy(
                            p -> p.getCustomer().getId(),
                            Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));

            // 고객별 인보이스(이미 invoiceDate asc 정렬). LinkedHashMap 으로 순서 유지.
            Map<Long, List<Invoice>> invoicesByCustomer = invoices.stream()
                    .collect(Collectors.groupingBy(
                            i -> i.getSalesOrder().getCustomer().getId(),
                            LinkedHashMap::new,
                            Collectors.toList()));

            int saved = 0;
            for (List<Invoice> customerInvoices : invoicesByCustomer.values()) {
                Customer customer = customerInvoices.get(0).getSalesOrder().getCustomer();
                BigDecimal paid = paidByCustomer.getOrDefault(customer.getId(), BigDecimal.ZERO);
                ArAgingBuckets b = ArAgingBuckets.compute(agingDate, customerInvoices, paid);
                if (b.total().signum() <= 0) {
                    continue; // 미수 없음 — 기록 생략.
                }
                arAgingRepository.save(ArAging.of(agingDate, customer,
                        b.bucket0to30(), b.bucket31to60(), b.bucket61to90(), b.bucketOver90(), b.total()));
                saved++;
            }
            log.info("[월말결산:채권노령화] date={} 고객수={} 미수고객저장={}",
                    agingDate, invoicesByCustomer.size(), saved);
            return RepeatStatus.FINISHED;
        };
    }
}
