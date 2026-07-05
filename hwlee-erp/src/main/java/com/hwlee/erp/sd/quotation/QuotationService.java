package com.hwlee.erp.sd.quotation;

import com.hwlee.erp.approval.ApprovalDocType;
import com.hwlee.erp.approval.ApprovalService;
import com.hwlee.erp.approval.dto.ApprovalResponse;
import com.hwlee.erp.approval.dto.ApprovalSubmitCommand;
import com.hwlee.erp.master.customer.Customer;
import com.hwlee.erp.master.customer.CustomerRepository;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemRepository;
import com.hwlee.erp.common.code.TransactionNumberGenerator;
import com.hwlee.erp.sd.quotation.dto.QuotationBulkRequest;
import com.hwlee.erp.sd.quotation.dto.QuotationBulkResponse;
import com.hwlee.erp.sd.quotation.dto.QuotationCreateRequest;
import com.hwlee.erp.sd.quotation.dto.QuotationLineRequest;
import com.hwlee.erp.sd.quotation.dto.QuotationResponse;
import com.hwlee.erp.sd.quotation.dto.QuotationUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuotationService {

    private final QuotationRepository repository;
    private final QuotationMapper mapper;
    private final CustomerRepository customerRepository;
    private final ItemRepository itemRepository;
    private final TransactionNumberGenerator numberGenerator;
    private final ApprovalService approvalService;

    @Transactional
    public QuotationResponse create(QuotationCreateRequest req) {
        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: id=" + req.customerId()));
        String number = numberGenerator.nextQuotationNumber(req.issuedDate());
        Quotation quotation = Quotation.draft(number, customer, req.issuedDate(), req.validUntil());
        addLines(quotation, req.lines());
        return mapper.toResponse(repository.save(quotation));
    }

    public QuotationResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    public Page<QuotationResponse> search(Specification<Quotation> spec, Pageable pageable) {
        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional
    public QuotationResponse update(Long id, QuotationUpdateRequest req) {
        Quotation quotation = getOrThrow(id);
        quotation.updateHeader(req.issuedDate(), req.validUntil());
        quotation.clearLines();
        addLines(quotation, req.lines());
        return mapper.toResponse(quotation);
    }

    @Transactional
    public QuotationResponse send(Long id) {
        Quotation quotation = getOrThrow(id);
        quotation.send();
        return mapper.toResponse(quotation);
    }

    /**
     * 견적 발송을 위한 결재 상신 — 작성 중(DRAFT) 견적을 전자결재에 올린다.
     * 최종 승인되면 {@code QuotationApprovalListener} 가 견적을 SENT 로 전이시킨다.
     */
    @Transactional
    public ApprovalResponse submitForApproval(Long id, String requester) {
        Quotation q = getOrThrow(id);
        if (q.getStatus() != QuotationStatus.DRAFT)
            throw new IllegalStateException("작성 중(DRAFT) 견적만 결재 상신할 수 있습니다. 현재: " + q.getStatus());
        if (q.getLines().isEmpty())
            throw new IllegalStateException("품목이 없는 견적은 상신할 수 없습니다.");
        return approvalService.submit(new ApprovalSubmitCommand(
                ApprovalDocType.QUOTATION, q.getId(), q.getNumber(),
                "견적 발송 승인 · " + q.getCustomer().getName() + " (" + q.getNumber() + ")",
                q.getTotalAmount(), requester));
    }

    @Transactional
    public QuotationResponse accept(Long id) {
        Quotation quotation = getOrThrow(id);
        quotation.accept();
        return mapper.toResponse(quotation);
    }

    @Transactional
    public QuotationResponse cancel(Long id) {
        Quotation quotation = getOrThrow(id);
        quotation.cancel();
        return mapper.toResponse(quotation);
    }

    /**
     * 일괄 작업 — 선택된 견적들에 같은 동작(발송/취소)을 적용한다.
     *
     * <p>상태 머신상 처리할 수 없는 건은 {@link IllegalStateException} 을 잡아 실패 목록에 담고
     * 나머지는 계속 진행한다(부분 성공). 잡은 예외는 다시 던지지 않으므로 성공분은 그대로 커밋된다.
     */
    @Transactional
    public QuotationBulkResponse bulk(QuotationBulkRequest req) {
        List<QuotationBulkResponse.Failure> failed = new ArrayList<>();
        int succeeded = 0;
        for (Long id : req.ids()) {
            Quotation quotation = repository.findById(id).orElse(null);
            if (quotation == null) {
                failed.add(new QuotationBulkResponse.Failure(id, null, "견적을 찾을 수 없습니다."));
                continue;
            }
            try {
                switch (req.action()) {
                    case SEND -> quotation.send();
                    case CANCEL -> quotation.cancel();
                }
                succeeded++;
            } catch (IllegalStateException e) {
                failed.add(new QuotationBulkResponse.Failure(id, quotation.getNumber(), e.getMessage()));
            }
        }
        return new QuotationBulkResponse(req.ids().size(), succeeded, failed);
    }

    private void addLines(Quotation quotation, List<QuotationLineRequest> lineReqs) {
        for (QuotationLineRequest lineReq : lineReqs) {
            Item item = itemRepository.findById(lineReq.itemId())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found: id=" + lineReq.itemId()));
            quotation.addLine(item, lineReq.quantity(), lineReq.unitPrice());
        }
    }

    private Quotation getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Quotation not found: id=" + id));
    }
}
