package com.hwlee.erp.common.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 시간 제공자. 도메인 코드는 {@link Clock} 을 주입받아 {@code LocalDate.now(clock)} 형태로 사용한다.
 * 테스트에서는 고정 Clock 으로 교체해 결정적(deterministic) 시나리오를 만든다.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
