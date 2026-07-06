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
 * 전자결재 최종 승인 → 견적 승인(APPROVED·발송대기) 전이.
 *
 * <p>결재 엔진(approval)은 견적(sd)을 직접 알지 못한다. 이 리스너가 승인 이벤트를 받아
 * 견적을 APPROVED 로 전이해 결재→문서의 역방향 의존을 끊는다. 승인 트랜잭션 커밋 직전(BEFORE_COMMIT)에
 * 같은 트랜잭션으로 실행돼 정합성을 보장한다(전이 실패 시 승인도 롤백).
 * 실제 고객 발송(SENT)은 담당자가 견적 화면에서 별도로 수행한다(승인과 발송 분리).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuotationApprovalListener {

    private final QuotationService quotationService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onApproved(ApprovalApprovedEvent event) {
        if (event.docType() != ApprovalDocType.QUOTATION) return;
        quotationService.approve(event.refId());
        log.info("결재 승인으로 견적 발송대기(APPROVED): {} (결재 {})", event.refNo(), event.approvalNumber());
    }
}
