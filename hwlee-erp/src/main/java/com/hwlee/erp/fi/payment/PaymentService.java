package com.hwlee.erp.fi.payment;

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
    private final Clock clock;

    /**
     * 등록 + 즉시 확정 + 자동 분개. 학습용 단순화 — DRAFT 상태로 두는 단계는 생략.
     */
    @Transactional
    public PaymentResponse createAndPost(PaymentCreateRequest req) {
        String number = numberGenerator.nextPaymentNumber(req.paymentDate());
        Payment payment = switch (req.type()) {
            case RECEIPT -> {
                if (req.customerId() == null) {
                    throw new IllegalArgumentException("RECEIPT 는 customerId 가 필수다.");
                }
                if (req.vendorId() != null) {
                    throw new IllegalArgumentException("RECEIPT 에는 vendorId 를 지정할 수 없다.");
                }
                Customer customer = customerRepository.findById(req.customerId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Customer not found: id=" + req.customerId()));
                yield Payment.receipt(number, customer, req.amount(), req.paymentDate(), req.description());
            }
            case DISBURSEMENT -> {
                if (req.vendorId() == null) {
                    throw new IllegalArgumentException("DISBURSEMENT 는 vendorId 가 필수다.");
                }
                if (req.customerId() != null) {
                    throw new IllegalArgumentException("DISBURSEMENT 에는 customerId 를 지정할 수 없다.");
                }
                Vendor vendor = vendorRepository.findById(req.vendorId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Vendor not found: id=" + req.vendorId()));
                yield Payment.disbursement(number, vendor, req.amount(), req.paymentDate(), req.description());
            }
        };

        payment.post(LocalDateTime.now(clock));
        Payment saved = repository.save(payment);

        // 자동 분개 — 같은 트랜잭션. 분개 실패 시 Payment 도 롤백.
        switch (saved.getType()) {
            case RECEIPT -> autoJournalService.createReceiptEntry(
                    saved.getId(), saved.getPaymentDate(), saved.getNumber(), saved.getAmount());
            case DISBURSEMENT -> autoJournalService.createDisbursementEntry(
                    saved.getId(), saved.getPaymentDate(), saved.getNumber(), saved.getAmount());
        }

        return mapper.toResponse(saved);
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
