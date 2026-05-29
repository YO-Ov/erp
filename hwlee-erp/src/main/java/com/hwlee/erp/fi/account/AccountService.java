package com.hwlee.erp.fi.account;

import com.hwlee.erp.fi.account.dto.AccountCreateRequest;
import com.hwlee.erp.fi.account.dto.AccountResponse;
import com.hwlee.erp.fi.account.dto.AccountUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정과목 서비스 — {@link com.hwlee.erp.master.department.DepartmentService} 와 같은 트리 마스터 패턴.
 *
 * <p>회계 모듈 외부({@link com.hwlee.erp.fi.journal AutoJournalService}, 시연 코드 등) 가
 * 계정을 코드로 조회할 수 있도록 {@link #getEntityByCode} 를 외부에 노출한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository repository;

    @Transactional
    public AccountResponse create(AccountCreateRequest req) {
        if (repository.existsByCode(req.code())) {
            throw new IllegalStateException("이미 등록된 계정 코드입니다: " + req.code());
        }
        Account parent = resolveParent(req.parentCode());
        Account account = Account.create(req.code(), req.name(), req.type(), parent, req.postable());
        return toResponse(repository.save(account));
    }

    public AccountResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    public AccountResponse findByCode(String code) {
        return toResponse(getEntityByCode(code));
    }

    public List<AccountResponse> findAll() {
        // 계정과목은 보통 수십~수백 개 — 페이징 없이 전체 반환.
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AccountResponse update(Long id, AccountUpdateRequest req) {
        Account account = getOrThrow(id);
        Account parent = resolveParent(req.parentCode());
        account.update(req.name(), parent, req.postable());
        return toResponse(account);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    /** 회계 모듈 내부(AutoJournalService 등) 가 계정 엔티티를 코드로 가져올 때 쓴다. */
    public Account getEntityByCode(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: code=" + code));
    }

    private Account getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: id=" + id));
    }

    private Account resolveParent(String parentCode) {
        if (parentCode == null || parentCode.isBlank()) {
            return null;
        }
        return repository.findByCode(parentCode)
                .orElseThrow(() -> new EntityNotFoundException("Parent account not found: code=" + parentCode));
    }

    private AccountResponse toResponse(Account a) {
        return new AccountResponse(
                a.getId(),
                a.getCode(),
                a.getName(),
                a.getType(),
                a.normalSide(),
                a.getParent() != null ? a.getParent().getCode() : null,
                a.isPostable(),
                a.getStatus(),
                a.getCreatedAt(),
                a.getCreatedBy(),
                a.getUpdatedAt(),
                a.getUpdatedBy()
        );
    }
}
