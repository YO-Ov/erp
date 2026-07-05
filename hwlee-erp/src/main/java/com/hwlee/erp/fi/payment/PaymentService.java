package com.hwlee.erp.fi.payment;

import com.hwlee.erp.approval.ApprovalDocType;
import com.hwlee.erp.approval.ApprovalService;
import com.hwlee.erp.approval.dto.ApprovalResponse;
import com.hwlee.erp.approval.dto.ApprovalSubmitCommand;
import com.hwlee.erp.common.code.TransactionNumberGenerator;
import com.hwlee.erp.fi.journal.AutoJournalService;
import com.hwlee.erp.fi.payment.dto.PaymentCreateRequest;
import com.hwlee.erp.fi.payment.dto.PaymentResponse;
import com.hwlee.erp.master.customer.Customer;
import com.hwlee.erp.master.customer.CustomerRepository;
import com.hwlee.erp.master.vendor.Vendor;
import com.hwlee.erp.master.vendor.VendorRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 입금/출금 서비스 — 등록 → 확정 시 자동 분개까지 한 트랜잭션으로.
 *
 * <p>이벤트로 분리하지 않은 이유: 입금/출금 자체가 본질적 회계 사건이라
 * "Payment 도메인 + FI 모듈" 사이의 모듈 분리 의의가 약하다. 같은 모듈 안의 직접 호출이 깔끔.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentMapper mapper;
    private final CustomerRepository customerRepository;
    private final VendorRepository vendorRepository;
    private final TransactionNumberGenerator numberGenerator;
    private final AutoJournalService autoJournalService;
    private final ApprovalService approvalService;
    private final Clock clock;

    /**
     * 등록 + 즉시 확정 + 자동 분개. 입금이나 결재 없이 처리하는 소액 출금 등의 직접 경로.
     * 지급(출금)에 전자결재를 태우려면 {@link #createDraft}→{@link #submitForApproval}→승인 콜백을 쓴다.
     */
    @Transactional
    public PaymentResponse createAndPost(PaymentCreateRequest req) {
        Payment saved = repository.save(build(req));
        saved.post(LocalDateTime.now(clock));
        postAutoJournal(saved);
        return mapper.toResponse(saved);
    }

    /** 결재 상신용 — 확정하지 않고 DRAFT 로만 저장한다. */
    @Transactional
    public PaymentResponse createDraft(PaymentCreateRequest req) {
        return mapper.toResponse(repository.save(build(req)));
    }

    /**
     * 지급(출금) 결재 상신 — DRAFT 지급을 전자결재에 올린다.
     * 최종 승인되면 {@code PaymentApprovalListener} 가 전기(POSTED)+출금 분개를 실행한다.
     */
    @Transactional
    public ApprovalResponse submitForApproval(Long id, String requester) {
        Payment p = getOrThrow(id);
        if (p.getType() != PaymentType.DISBURSEMENT)
            throw new IllegalStateException("지급(출금) 건만 결재 상신할 수 있습니다.");
        if (p.getStatus() != PaymentStatus.DRAFT)
            throw new IllegalStateException("작성 중(DRAFT) 지급만 결재 상신할 수 있습니다. 현재: " + p.getStatus());
        return approvalService.submit(new ApprovalSubmitCommand(
                ApprovalDocType.PAYMENT, p.getId(), p.getNumber(),
                "지급결의 · " + p.getVendor().getName() + " (" + p.getNumber() + ")",
                p.getAmount(), requester));
    }

    /** 결재 최종 승인 콜백 — 전기(POSTED) + 출금 자동분개. */
    @Transactional
    public void postByApproval(Long id) {
        Payment p = getOrThrow(id);
        p.post(LocalDateTime.now(clock));
        postAutoJournal(p);
    }

    /** 채번 + 팩토리 생성(확정 전). type/party 정합성 검증 포함. */
    private Payment build(PaymentCreateRequest req) {
        String number = numberGenerator.nextPaymentNumber(req.paymentDate());
        return switch (req.type()) {
            case RECEIPT -> {
                if (req.customerId() == null)
                    throw new IllegalArgumentException("RECEIPT 는 customerId 가 필수다.");
                if (req.vendorId() != null)
                    throw new IllegalArgumentException("RECEIPT 에는 vendorId 를 지정할 수 없다.");
                Customer customer = customerRepository.findById(req.customerId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Customer not found: id=" + req.customerId()));
                yield Payment.receipt(number, customer, req.amount(), req.paymentDate(), req.description());
            }
            case DISBURSEMENT -> {
                if (req.vendorId() == null)
                    throw new IllegalArgumentException("DISBURSEMENT 는 vendorId 가 필수다.");
                if (req.customerId() != null)
                    throw new IllegalArgumentException("DISBURSEMENT 에는 customerId 를 지정할 수 없다.");
                Vendor vendor = vendorRepository.findById(req.vendorId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Vendor not found: id=" + req.vendorId()));
                yield Payment.disbursement(number, vendor, req.amount(), req.paymentDate(), req.description());
            }
        };
    }

    /** 확정 시 자동 분개 — 같은 트랜잭션. 분개 실패 시 Payment 도 롤백. */
    private void postAutoJournal(Payment saved) {
        switch (saved.getType()) {
            case RECEIPT -> autoJournalService.createReceiptEntry(
                    saved.getId(), saved.getPaymentDate(), saved.getNumber(), saved.getAmount());
            case DISBURSEMENT -> autoJournalService.createDisbursementEntry(
                    saved.getId(), saved.getPaymentDate(), saved.getNumber(), saved.getAmount());
        }
    }

    public PaymentResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    public PaymentResponse findByNumber(String number) {
        return mapper.toResponse(repository.findByNumber(number)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: number=" + number)));
    }

    public Page<PaymentResponse> search(Specification<Payment> spec, Pageable pageable) {
        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    private Payment getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: id=" + id));
    }
}
