package com.hwlee.erp.mm.integration.approval;

import com.hwlee.erp.approval.ApprovalDocType;
import com.hwlee.erp.approval.event.ApprovalApprovedEvent;
import com.hwlee.erp.mm.purchaseorder.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 전자결재 최종 승인 → 구매발주 확정(CONFIRMED) 전이.
 *
 * <p>결재 엔진(approval)은 구매발주(mm)를 직접 알지 못한다. 이 리스너가 승인 이벤트를 받아
 * 발주를 CONFIRMED 로 전이해 결재→문서의 역방향 의존을 끊는다. 승인 트랜잭션 커밋 직전
 * (BEFORE_COMMIT)에 같은 트랜잭션으로 실행돼 정합성을 보장한다(전이 실패 시 승인도 롤백).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderApprovalListener {

    private final PurchaseOrderService purchaseOrderService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onApproved(ApprovalApprovedEvent event) {
        if (event.docType() != ApprovalDocType.PURCHASE_ORDER) return;
        purchaseOrderService.confirmByApproval(event.refId());
        log.info("결재 승인으로 구매발주 확정(CONFIRMED): {} (결재 {})", event.refNo(), event.approvalNumber());
    }
}
