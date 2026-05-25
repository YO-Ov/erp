package com.hwlee.erp.common.code;

import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비즈니스 코드 발급기 (마스터 + 트랜잭션 공용).
 *
 * <p>형식: {@code <PREFIX>-<PERIOD_KEY>-<0-padded N digits>}
 * — 마스터(연 단위): {@code CUST-2026-0042} (4자리)
 * — 트랜잭션(일 단위): {@code SO-20260524-001} (3자리)
 *
 * <p>{@link Propagation#REQUIRES_NEW} 로 별도 트랜잭션에서 발급한다.
 * 호출 측 트랜잭션이 실패해도 발급된 번호는 커밋되어 "번호 구멍" 이 생길 수 있지만,
 * 동시성 안전성이 우선이라 이 트레이드오프를 수용한다.
 *
 * <p>두 단계 동시성 처리:
 * <ol>
 *   <li>(prefix, periodKey) 행이 이미 있으면 {@code SELECT ... FOR UPDATE} 로 점유 후 증가.
 *   <li>없으면 INSERT 를 시도하고, UNIQUE 위반이 나면 다른 트랜잭션이 먼저 만든 것이므로
 *       다시 락 걸고 조회한다.
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class CodeGenerator {

    private static final String MASTER_FORMAT = "%s-%s-%04d";
    private static final String TRANSACTION_FORMAT = "%s-%s-%03d";

    private final CodeSequenceRepository repository;
    private final Clock clock;

    /**
     * 마스터용 — 연 단위 periodKey 로 4자리 일련번호 발급.
     * 예: {@code nextCode("CUST")} → {@code "CUST-2026-0001"}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextCode(String prefix) {
        String periodKey = String.valueOf(LocalDate.now(clock).getYear());
        int issued = issue(prefix, periodKey);
        return String.format(MASTER_FORMAT, prefix, periodKey, issued);
    }

    /**
     * 트랜잭션용 — 일 단위 periodKey 로 3자리 일련번호 발급.
     * 예: {@code nextTransactionCode("SO", "20260524")} → {@code "SO-20260524-001"}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextTransactionCode(String prefix, String periodKey) {
        int issued = issue(prefix, periodKey);
        return String.format(TRANSACTION_FORMAT, prefix, periodKey, issued);
    }

    private int issue(String prefix, String periodKey) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix 는 비어 있을 수 없다.");
        }
        if (periodKey == null || periodKey.isBlank()) {
            throw new IllegalArgumentException("periodKey 는 비어 있을 수 없다.");
        }
        CodeSequence sequence = repository.findForUpdate(prefix, periodKey)
                .orElseGet(() -> createOrLockExisting(prefix, periodKey));
        return sequence.issueAndIncrement();
    }

    /**
     * (prefix, periodKey) 행이 없을 때 호출된다.
     * 동시에 여러 트랜잭션이 이 분기에 들어오면 한 트랜잭션만 INSERT 성공하고,
     * 나머지는 UNIQUE 위반 → 락 걸고 재조회 흐름으로 합류한다.
     */
    private CodeSequence createOrLockExisting(String prefix, String periodKey) {
        try {
            return repository.saveAndFlush(CodeSequence.initial(prefix, periodKey));
        } catch (DataIntegrityViolationException duplicate) {
            return repository.findForUpdate(prefix, periodKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "code_sequence 초기화 경합 처리 실패: prefix=" + prefix
                                    + ", periodKey=" + periodKey,
                            duplicate));
        }
    }
}
