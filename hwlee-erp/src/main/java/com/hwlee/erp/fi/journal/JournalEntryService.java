package com.hwlee.erp.fi.journal;

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
    private final Clock clock;

    /**
     * 수동 전표 생성 + 즉시 확정. 차/대 검증은 {@link JournalEntry#post} 가 수행.
     */
    @Transactional
    public JournalEntryResponse createManual(JournalEntryCreateRequest req) {
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
        entry.post(LocalDateTime.now(clock));
        return mapper.toResponse(repository.save(entry));
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
