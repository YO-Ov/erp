package com.hwlee.erp.fi.integration.mm;

import com.hwlee.erp.fi.journal.AutoJournalService;
import com.hwlee.erp.mm.goodsreceipt.event.GoodsReceiptPostedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * MM 의 입고 확정 사건 → FI 의 매입 자동 분개 (Phase 5).
 *
 * <p>패키지 {@code fi/integration/mm/} — FI 가 MM 사건을 구독. 의존 방향 FI → MM 단방향.
 *
 * <p>{@code @Order} 미지정 — 입고 확정 사건은 이 리스너 하나만 듣는다.
 * 매입 단가는 사용자가 입력한 {@code unitCost} 라 별도 조회가 필요 없다 (매출원가와 다른 점).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class PurchaseAccountingListener {

    private final AutoJournalService autoJournal;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void onPosted(GoodsReceiptPostedEvent event) {
        log.info("입고 확정 사건 수신: grId={}, number={}, lines={}",
                event.goodsReceiptId(), event.number(), event.lines().size());
        autoJournal.createPurchaseEntry(event);
    }
}
