package com.hwlee.erp.fi.integration.hr;

import com.hwlee.erp.fi.journal.AutoJournalService;
import com.hwlee.erp.hr.payroll.event.PayrollConfirmedEvent;
import com.hwlee.erp.hr.payroll.event.PayrollPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * HR 의 급여 확정/지급 사건 → FI 의 인건비 자동 분개 (Phase 7).
 *
 * <p>패키지 위치 {@code fi/integration/hr/} 가 의존 방향을 표현 — "HR 로부터 들어오는 통합".
 * Phase 5 의 {@code fi/integration/sd/SalesAccountingListener} 와 같은 규칙
 * (이벤트는 발행자 HR 이, 리스너는 수신자 FI 가 소유).
 *
 * <p>{@code @TransactionalEventListener(BEFORE_COMMIT)} 이므로 급여 확정/지급 트랜잭션과
 * 같은 트랜잭션 안에서 실행 — 분개 실패(차/대 불일치 등)는 급여 확정/지급 자체를 롤백시킨다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class PayrollAccountingListener {

    private final AutoJournalService autoJournal;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void onConfirmed(PayrollConfirmedEvent event) {
        log.info("급여 확정 사건 수신: payrollRunId={}, number={}, gross={}",
                event.payrollRunId(), event.number(), event.totalGross());
        autoJournal.createPayrollEntry(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void onPaid(PayrollPaidEvent event) {
        log.info("급여 지급 사건 수신: payrollRunId={}, number={}, net={}",
                event.payrollRunId(), event.number(), event.totalNet());
        autoJournal.createSalaryPaymentEntry(event);
    }
}
