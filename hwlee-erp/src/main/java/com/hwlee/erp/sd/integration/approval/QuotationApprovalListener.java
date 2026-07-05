package com.hwlee.erp.sd.integration.approval;

import com.hwlee.erp.approval.ApprovalDocType;
import com.hwlee.erp.approval.event.ApprovalApprovedEvent;
import com.hwlee.erp.sd.quotation.QuotationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 전자결재 최종 승인 → 견적 발송(SENT) 전이.
 *
 * <p>결재 엔진(approval)은 견적(sd)을 직접 알지 못한다. 이 리스너가 승인 이벤트를 받아
 * 견적을 발송 처리해 결재→문서의 역방향 의존을 끊는다. 승인 트랜잭션 커밋 직전(BEFORE_COMMIT)에
 * 같은 트랜잭션으로 실행돼 정합성을 보장한다(발송 실패 시 승인도 롤백).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuotationApprovalListener {

    private final QuotationService quotationService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onApproved(ApprovalApprovedEvent event) {
        if (event.docType() != ApprovalDocType.QUOTATION) return;
        quotationService.send(event.refId());
        log.info("결재 승인으로 견적 발송: {} (결재 {})", event.refNo(), event.approvalNumber());
    }
}
