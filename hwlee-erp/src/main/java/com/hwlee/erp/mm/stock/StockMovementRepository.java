package com.hwlee.erp.mm.stock;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockMovementRepository
        extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {

    /**
     * 특정 출처(예: GoodsIssue) + 사유(예: GOODS_ISSUE)로 이동 원장을 조회한다.
     *
     * <p>Phase 5 의 매출원가 분개가 출하 단가를 구할 때 사용:
     * {@code findByRefTypeAndRefIdAndReason("GI", giId, GOODS_ISSUE)} → 라인별 {@code unitCost × |qtyDelta|} 합산.
     */
    List<StockMovement> findByRefTypeAndRefIdAndReason(String refType, Long refId, MovementReason reason);
}
