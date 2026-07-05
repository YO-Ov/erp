package com.hwlee.erp.fi.integration.approval;

import com.hwlee.erp.approval.ApprovalDocType;
import com.hwlee.erp.approval.event.ApprovalApprovedEvent;
import com.hwlee.erp.approval.event.ApprovalRejectedEvent;
import com.hwlee.erp.fi.credit.CreditLimitRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 전자결재 결과 → 여신 상향 요청 상태 반영.
 *
 * <p>승인 시 요청을 APPROVED 로 확정하고 고객 한도를 올리며, 반려 시 REJECTED 로 종결한다.
 * 승인 트랜잭션과 한 몸(BEFORE_COMMIT)이라 한도 반영이 실패하면 승인도 롤백된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditApprovalListener {

    private final CreditLimitRequestService creditLimitRequestService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onApproved(ApprovalApprovedEvent event) {
        if (event.docType() != ApprovalDocType.CREDIT_LIMIT) return;
        creditLimitRequestService.applyApproval(event.refId(), event.decidedBy());
        log.info("결재 승인으로 여신 상향 확정: {} (결재 {})", event.refNo(), event.approvalNumber());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onRejected(ApprovalRejectedEvent event) {
        if (event.docType() != ApprovalDocType.CREDIT_LIMIT) return;
        creditLimitRequestService.applyRejection(event.refId(), event.decidedBy(), event.reason());
        log.info("결재 반려로 여신 상향 종결: {} (결재 {})", event.refNo(), event.approvalNumber());
    }
}
