package com.hwlee.erp.fi.integration.approval;

import com.hwlee.erp.approval.ApprovalDocType;
import com.hwlee.erp.approval.event.ApprovalApprovedEvent;
import com.hwlee.erp.fi.journal.JournalEntryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 전자결재 최종 승인 → 수동 전표 전기(POSTED).
 *
 * <p>승인 트랜잭션 커밋 직전(BEFORE_COMMIT)에 같은 트랜잭션으로 실행된다. 전기 시 차대 균형
 * 검증이 함께 돌아, 불균형이면 승인도 롤백된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JournalEntryApprovalListener {

    private final JournalEntryService journalEntryService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onApproved(ApprovalApprovedEvent event) {
        if (event.docType() != ApprovalDocType.JOURNAL) return;
        journalEntryService.postByApproval(event.refId());
        log.info("결재 승인으로 전표 전기: {} (결재 {})", event.refNo(), event.approvalNumber());
    }
}
