package com.hwlee.erp.common.code;

import static org.assertj.core.api.Assertions.assertThat;

import com.hwlee.erp.TestcontainersConfiguration;
import java.time.Clock;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * CodeGenerator 동시성 보장 검증.
 *
 * <p>업무 규칙: 동시에 N 개 발급 요청이 들어와도 코드는 절대 중복되지 않는다.
 *
 * <p>전제: (prefix, year) 시퀀스 행은 미리 만들어져 있다고 가정한다.
 * 운영에서 행 초기화는 부팅 시 한 번만 발생하는 이벤트이고,
 * 그 충돌 처리는 별도 단위 테스트 ({@code CodeGeneratorInitRaceTest}) 에서 다룬다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CodeGeneratorConcurrencyTest {

    private static final String PREFIX = "TEST";

    @Autowired
    CodeGenerator codeGenerator;

    @Autowired
    CodeSequenceRepository repository;

    @Autowired
    Clock clock;

    @BeforeEach
    void seedSequenceRow() {
        int year = LocalDate.now(clock).getYear();
        repository.findAll().stream()
                .filter(s -> s.getPrefix().equals(PREFIX) && s.getYear() == year)
                .findFirst()
                .ifPresentOrElse(s -> {}, () -> repository.save(CodeSequence.initial(PREFIX, year)));
    }

    @Test
    @DisplayName("코드 생성은 동시 요청에서도 중복되지 않는다")
    void 코드_생성은_동시_요청에서도_중복되지_않는다() throws InterruptedException, ExecutionException {
        // given — 같은 prefix 로 50개 동시 발급 요청
        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);

        List<Future<String>> futures = IntStream.range(0, threads).mapToObj(i -> pool.submit(() -> {
            startGate.await();
            return codeGenerator.nextCode(PREFIX);
        })).toList();

        // when — 모든 스레드 동시 출발
        startGate.countDown();
        pool.shutdown();
        boolean finished = pool.awaitTermination(60, TimeUnit.SECONDS);

        // then
        assertThat(finished).as("모든 스레드가 60초 안에 끝나야 한다").isTrue();

        Set<String> issued = new HashSet<>();
        for (Future<String> f : futures) {
            issued.add(f.get());
        }
        assertThat(issued)
                .as("발급된 코드는 모두 unique 해야 한다 (락이 없으면 여기서 깨진다)")
                .hasSize(threads);
    }
}
