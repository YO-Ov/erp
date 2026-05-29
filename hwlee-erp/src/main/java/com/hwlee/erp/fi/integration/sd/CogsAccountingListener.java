package com.hwlee.erp.fi.integration.sd;

import com.hwlee.erp.fi.journal.AutoJournalService;
import com.hwlee.erp.sd.delivery.event.DeliveryShippedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * SD 의 출하 사건 → FI 의 매출원가 자동 분개 (Phase 5).
 *
 * <p>핵심 — {@link DeliveryShippedEvent} 를 두 모듈이 동시에 듣는다:
 * <ol>
 *   <li>{@code mm.integration.sd.DeliveryEventListener.onShipped} ({@code @Order(10)}) — GoodsIssue.post + StockMovement 적재</li>
 *   <li>{@code fi.integration.sd.CogsAccountingListener.onShipped} ({@code @Order(20)}) — 매출원가 분개</li>
 * </ol>
 * 회계 리스너가 먼저 돌면 StockMovement 의 {@code unit_cost} 가 아직 없어 매출원가가 0이 된다.
 * {@code @Order} 가 정합성을 좌우 — Phase 5 의 새 학습 포인트.
 *
 * <p>같은 트랜잭션({@code BEFORE_COMMIT})이므로 분개 실패는 출하·재고 차감·SO 라인 누적까지 모두 롤백.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class CogsAccountingListener {

    private final AutoJournalService autoJournal;

    @Order(20)   // 재고 리스너(10) 보다 나중. unit_cost 가 박힌 뒤에야 매출원가 계산 가능.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void onShipped(DeliveryShippedEvent event) {
        log.info("출하 확정 사건 수신(매출원가용): deliveryId={}", event.deliveryId());
        autoJournal.createCogsEntry(event);
    }
}
