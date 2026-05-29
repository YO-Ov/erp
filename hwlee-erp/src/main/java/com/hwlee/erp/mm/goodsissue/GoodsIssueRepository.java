package com.hwlee.erp.mm.goodsissue;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GoodsIssueRepository
        extends JpaRepository<GoodsIssue, Long>, JpaSpecificationExecutor<GoodsIssue> {

    /** 출하 취소 시 원천 Delivery 로부터 자동 생성된 GI 를 역추적한다 (Phase 4). */
    Optional<GoodsIssue> findByDeliveryId(Long deliveryId);
}
