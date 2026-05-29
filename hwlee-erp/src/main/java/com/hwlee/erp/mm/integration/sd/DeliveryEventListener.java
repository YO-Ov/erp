package com.hwlee.erp.mm.integration.sd;

import com.hwlee.erp.mm.goodsissue.GoodsIssueService;
import com.hwlee.erp.sd.delivery.event.DeliveryCancelledEvent;
import com.hwlee.erp.sd.delivery.event.DeliveryShippedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * SD 의 출하 사건을 받아 MM 재고를 움직이는 통합 리스너 (Phase 4).
 *
 * <p>패키지 위치 {@code mm/integration/sd/} 가 의존 방향을 표현한다 — "SD 로부터 들어오는 통합".
 * SD 는 누가 듣는지 모르고, MM 만이 SD 의 이벤트 타입을 안다 → 단방향 의존 {@code MM → SD}.
 *
 * <p><b>{@code @TransactionalEventListener(BEFORE_COMMIT)}</b> 가 핵심:
 * <ul>
 *   <li>발행자(DeliveryService) 의 트랜잭션 <b>commit 직전</b> 에, <b>같은 트랜잭션</b> 안에서 실행된다.</li>
 *   <li>리스너가 던진 예외(가용 재고 부족 등)는 commit 전에 발생하므로 전체 트랜잭션이 롤백된다
 *       — 출하·SO 라인·재고가 모두 시도 전 상태로 복귀.</li>
 *   <li>리스너에 {@code @Transactional} 을 따로 붙이지 않는다 — 호출하는 서비스 메서드가
 *       {@code @Transactional(REQUIRED)} 라 자동으로 이 트랜잭션에 참여한다.</li>
 * </ul>
 * {@code AFTER_COMMIT}(기본값) 이었다면 출하는 이미 커밋된 뒤라 재고 차감 실패 시 부분 상태가 남는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class DeliveryEventListener {

    private final GoodsIssueService goodsIssueService;

    /**
     * {@code @Order(10)} 으로 회계 리스너({@code @Order(20)}, Phase 5) 보다 먼저 실행되도록 못박는다.
     * 회계의 매출원가 분개는 StockMovement 의 {@code unit_cost} 가 박혀 있어야 계산 가능 —
     * 즉 GoodsIssue.post 가 끝난 뒤에 회계가 동작해야 한다. 순서가 정합성을 좌우.
     */
    @Order(10)
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void onShipped(DeliveryShippedEvent event) {
        log.info("출하 확정 사건 수신: deliveryId={}, warehouseId={}, lines={}",
                event.deliveryId(), event.warehouseId(), event.lines().size());
        goodsIssueService.createAndPostFromDelivery(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void onCancelled(DeliveryCancelledEvent event) {
        log.info("출하 취소 사건 수신: deliveryId={}", event.deliveryId());
        goodsIssueService.cancelByDeliveryId(event.deliveryId());
    }
}
