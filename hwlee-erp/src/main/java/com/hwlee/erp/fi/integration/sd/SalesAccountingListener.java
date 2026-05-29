package com.hwlee.erp.fi.integration.sd;

import com.hwlee.erp.fi.journal.AutoJournalService;
import com.hwlee.erp.sd.invoice.event.InvoiceIssuedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * SD 의 인보이스 발행 사건 → FI 의 매출 자동 분개 (Phase 5).
 *
 * <p>패키지 위치 {@code fi/integration/sd/} 가 의존 방향을 표현 — "SD 로부터 들어오는 통합".
 * Phase 4 의 {@code mm/integration/sd/DeliveryEventListener} 와 같은 규칙 (이벤트는 발행자가, 리스너는 수신자가 소유).
 *
 * <p>{@code @TransactionalEventListener(BEFORE_COMMIT)} 이므로 인보이스 발행 트랜잭션과
 * 같은 트랜잭션 안에서 실행 — 분개 실패(차/대 불일치 등)는 인보이스 발행 자체를 롤백시킨다.
 *
 * <p>{@code @Order} 미지정. 인보이스 발행 사건은 이 리스너 하나만 듣는다 — 순서 경합 없음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class SalesAccountingListener {

    private final AutoJournalService autoJournal;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void onIssued(InvoiceIssuedEvent event) {
        log.info("인보이스 발행 사건 수신: invoiceId={}, number={}, total={}",
                event.invoiceId(), event.number(), event.totalAmount());
        autoJournal.createSalesEntry(event);
    }
}
