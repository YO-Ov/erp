package com.hwlee.erp.fi.journal;

import com.hwlee.erp.approval.ApprovalDocType;
import com.hwlee.erp.approval.ApprovalService;
import com.hwlee.erp.approval.dto.ApprovalResponse;
import com.hwlee.erp.approval.dto.ApprovalSubmitCommand;
import com.hwlee.erp.common.code.TransactionNumberGenerator;
import com.hwlee.erp.fi.account.Account;
import com.hwlee.erp.fi.account.AccountService;
import com.hwlee.erp.fi.journal.dto.JournalEntryCreateRequest;
import com.hwlee.erp.fi.journal.dto.JournalEntryResponse;
import com.hwlee.erp.fi.journal.dto.JournalLineRequest;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전표 서비스 — 수동 전표 등록/확정 + 조회.
 *
 * <p>자동 분개(매출/매입/매출원가/입금)는 이 서비스를 거치지 않고
 * {@link AutoJournalService} 가 도메인 메서드를 직접 호출해 생성한다.
 * 이 서비스는 사람이 직접 입력하는 수동 전표(MANUAL) 와 공통 조회를 담당.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JournalEntryService {

    private final JournalEntryRepository repository;
    private final JournalEntryMapper mapper;
    private final AccountService accountService;
    private final TransactionNumberGenerator numberGenerator;
    private final ApprovalService approvalService;
    private final Clock clock;

    /**
     * 수동 전표 생성 + 즉시 확정. 차/대 검증은 {@link JournalEntry#post} 가 수행.
     * 결재를 태우려면 {@link #createManualDraft}→{@link #submitForApproval}→승인 콜백을 쓴다.
     */
    @Transactional
    public JournalEntryResponse createManual(JournalEntryCreateRequest req) {
        JournalEntry entry = buildManual(req);
        entry.post(LocalDateTime.now(clock));
        return mapper.toResponse(repository.save(entry));
    }

    /** 결재 상신용 — 확정하지 않고 DRAFT 로만 저장한다. */
    @Transactional
    public JournalEntryResponse createManualDraft(JournalEntryCreateRequest req) {
        return mapper.toResponse(repository.save(buildManual(req)));
    }

    /**
     * 수동 전표 결재 상신 — DRAFT 전표를 전자결재에 올린다(차변 합계가 전결 금액 기준).
     * 최종 승인되면 {@code JournalEntryApprovalListener} 가 전기(POSTED)한다.
     */
    @Transactional
    public ApprovalResponse submitForApproval(Long id, String requester) {
        JournalEntry entry = getOrThrow(id);
        if (entry.getSourceType() != JournalSource.MANUAL)
            throw new IllegalStateException("수동 전표만 결재 상신할 수 있습니다.");
        if (entry.getStatus() != JournalEntryStatus.DRAFT)
            throw new IllegalStateException("작성 중(DRAFT) 전표만 결재 상신할 수 있습니다. 현재: " + entry.getStatus());
        return approvalService.submit(new ApprovalSubmitCommand(
                ApprovalDocType.JOURNAL, entry.getId(), entry.getNumber(),
                "수동 전표 · " + (entry.getDescription() == null ? entry.getNumber() : entry.getDescription()),
                entry.totalDebit(), requester));
    }

    /** 결재 최종 승인 콜백 — 전기(POSTED). 차대 균형 검증도 이 시점에 수행. */
    @Transactional
    public void postByApproval(Long id) {
        getOrThrow(id).post(LocalDateTime.now(clock));
    }

    /** 채번 + 라인 구성(확정 전). */
    private JournalEntry buildManual(JournalEntryCreateRequest req) {
        String number = numberGenerator.nextJournalEntryNumber(req.entryDate());
        JournalEntry entry = JournalEntry.draft(
                number, req.entryDate(), req.description(), JournalSource.MANUAL, null);
        for (JournalLineRequest lineReq : req.lines()) {
            Account account = accountService.getEntityByCode(lineReq.accountCode());
            BigDecimal debit = lineReq.debit();
            BigDecimal credit = lineReq.credit();
            if (debit.signum() > 0) {
                entry.addDebit(account, debit);
            } else if (credit.signum() > 0) {
                entry.addCredit(account, credit);
            } else {
                throw new IllegalArgumentException(
                        "라인은 차변 또는 대변 중 한 쪽이 > 0 이어야 한다. accountCode=" + lineReq.accountCode());
            }
        }
        return entry;
    }

    public JournalEntryResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    public JournalEntryResponse findByNumber(String number) {
        return mapper.toResponse(repository.findByNumber(number)
                .orElseThrow(() -> new EntityNotFoundException("JournalEntry not found: number=" + number)));
    }

    public Page<JournalEntryResponse> search(Specification<JournalEntry> spec, Pageable pageable) {
        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional
    public JournalEntryResponse cancel(Long id) {
        JournalEntry entry = getOrThrow(id);
        entry.cancel();
        return mapper.toResponse(entry);
    }

    private JournalEntry getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("JournalEntry not found: id=" + id));
    }
}
