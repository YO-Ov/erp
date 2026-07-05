package com.hwlee.erp.fi.integration.approval;

import com.hwlee.erp.approval.ApprovalDocType;
import com.hwlee.erp.approval.event.ApprovalApprovedEvent;
import com.hwlee.erp.fi.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 전자결재 최종 승인 → 지급(출금) 전기(POSTED) + 출금 자동분개.
 *
 * <p>승인 트랜잭션 커밋 직전(BEFORE_COMMIT)에 같은 트랜잭션으로 실행돼, 전기·분개가 실패하면
 * 승인도 함께 롤백된다. 결재(approval)→지급(fi) 역방향 의존을 이벤트로 끊는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentApprovalListener {

    private final PaymentService paymentService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onApproved(ApprovalApprovedEvent event) {
        if (event.docType() != ApprovalDocType.PAYMENT) return;
        paymentService.postByApproval(event.refId());
        log.info("결재 승인으로 지급 전기: {} (결재 {})", event.refNo(), event.approvalNumber());
    }
}
