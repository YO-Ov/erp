package com.hwlee.erp.common.code;

import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 마스터 비즈니스 코드 발급기.
 *
 * <p>형식: {@code <PREFIX>-<YEAR>-<0-padded 4 digits>} (예: {@code CUST-2026-0042})
 *
 * <p>{@link Propagation#REQUIRES_NEW} 로 별도 트랜잭션에서 발급한다.
 * 호출 측 트랜잭션이 실패해도 발급된 번호는 커밋되어 "번호 구멍" 이 생길 수 있지만,
 * 동시성 안전성이 우선이라 이 트레이드오프를 수용한다.
 *
 * <p>두 단계 동시성 처리:
 * <ol>
 *   <li>(prefix, year) 행이 이미 있으면 {@code SELECT ... FOR UPDATE} 로 점유 후 증가.
 *   <li>없으면 INSERT 를 시도하고, UNIQUE 위반이 나면 다른 트랜잭션이 먼저 만든 것이므로
 *       다시 락 걸고 조회한다.
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class CodeGenerator {

    private static final String CODE_FORMAT = "%s-%d-%04d";

    private final CodeSequenceRepository repository;
    private final Clock clock;

    /**
     * 주어진 prefix 의 다음 코드를 발급한다.
     * 호출 측은 트랜잭션 안이든 밖이든 상관 없다(이 메서드가 자체 트랜잭션을 연다).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextCode(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix 는 비어 있을 수 없다.");
        }
        int year = LocalDate.now(clock).getYear();

        CodeSequence sequence = repository.findForUpdate(prefix, year)
                .orElseGet(() -> createOrLockExisting(prefix, year));

        int issued = sequence.issueAndIncrement();
        return String.format(CODE_FORMAT, prefix, year, issued);
    }

    /**
     * (prefix, year) 행이 없을 때 호출된다.
     * 동시에 여러 트랜잭션이 이 분기에 들어오면 한 트랜잭션만 INSERT 성공하고,
     * 나머지는 UNIQUE 위반 → 락 걸고 재조회 흐름으로 합류한다.
     */
    private CodeSequence createOrLockExisting(String prefix, int year) {
        try {
            return repository.saveAndFlush(CodeSequence.initial(prefix, year));
        } catch (DataIntegrityViolationException duplicate) {
            return repository.findForUpdate(prefix, year)
                    .orElseThrow(() -> new IllegalStateException(
                            "code_sequence 초기화 경합 처리 실패: prefix=" + prefix + ", year=" + year,
                            duplicate));
        }
    }
}
