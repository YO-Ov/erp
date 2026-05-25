package com.hwlee.erp.sd;

import static org.assertj.core.api.Assertions.assertThat;

import com.hwlee.erp.TestcontainersConfiguration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 트랜잭션 번호 동시성 보장 — 같은 날짜에 동시 요청해도 절대 중복되지 않는다.
 *
 * <p>Phase 1 의 코드 생성 락 패턴을 그대로 재사용 — 일 단위 periodKey 로만 분리.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TransactionNumberConcurrencyTest {

    @Autowired
    TransactionNumberGenerator generator;

    @Test
    @DisplayName("같은 날짜에 동시 수주 등록시 번호가 중복되지 않는다")
    void 같은_날짜에_동시_수주_등록시_번호가_중복되지_않는다() throws InterruptedException, ExecutionException {
        int threads = 50;
        LocalDate today = LocalDate.now();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);

        List<Future<String>> futures = IntStream.range(0, threads).mapToObj(i -> pool.submit(() -> {
            startGate.await();
            return generator.nextSalesOrderNumber(today);
        })).toList();

        startGate.countDown();
        pool.shutdown();
        boolean finished = pool.awaitTermination(60, TimeUnit.SECONDS);
        assertThat(finished).as("모든 스레드가 60초 안에 끝나야 한다").isTrue();

        Set<String> issued = new HashSet<>();
        for (Future<String> f : futures) {
            issued.add(f.get());
        }
        assertThat(issued)
                .as("발급된 번호는 모두 unique 해야 한다")
                .hasSize(threads);
    }
}
