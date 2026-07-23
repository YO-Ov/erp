package com.hwlee.erp.master.customer;

import com.hwlee.erp.common.code.CodeGenerator;
import com.hwlee.erp.master.customer.dto.CustomerContactRequest;
import com.hwlee.erp.master.customer.dto.CustomerContactResponse;
import com.hwlee.erp.master.customer.dto.CustomerCreateRequest;
import com.hwlee.erp.master.customer.dto.CustomerResponse;
import com.hwlee.erp.master.customer.dto.CustomerUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
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
public class CustomerService {

    static final String CODE_PREFIX = "CUST";

    private final CustomerRepository repository;
    private final CustomerMapper mapper;
    private final CodeGenerator codeGenerator;

    @Transactional
    public CustomerResponse create(CustomerCreateRequest req) {
        if (repository.existsByBusinessNo(req.businessNo())) {
            throw new IllegalStateException("이미 등록된 사업자번호입니다: " + req.businessNo());
        }
        String code = codeGenerator.nextCode(CODE_PREFIX);
        // 신규 고객은 신용한도 0(현금거래)으로 시작 — 한도 부여/상향은 재무의 여신 승인으로만.
        Customer customer = Customer.create(
                code,
                req.name(),
                req.businessNo(),
                req.address(),
                java.math.BigDecimal.ZERO,
                req.paymentTerms()
        );
        Customer saved = repository.save(customer);
        return mapper.toResponse(saved);
    }

    public CustomerResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    public CustomerResponse findByCode(String code) {
        Customer customer = repository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: code=" + code));
        return mapper.toResponse(customer);
    }

    public Page<CustomerResponse> search(Specification<Customer> spec, Pageable pageable) {
        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerUpdateRequest req) {
        Customer customer = getOrThrow(id);
        customer.updateBasicInfo(req.name(), req.address(), req.paymentTerms());
        return mapper.toResponse(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = getOrThrow(id);
        repository.delete(customer); // @SQLDelete 에 의해 Soft Delete 로 동작
    }

    // ── 담당자(연락처) ─────────────────────────────────────
    // 변경은 항상 Customer 애그리거트를 통해서만(대표 1명 불변식은 도메인이 보장).

    /** 고객의 담당자 목록(대표 담당자 우선). */
    public List<CustomerContactResponse> getContacts(Long customerId) {
        return mapper.toContactResponses(getOrThrow(customerId).getContacts());
    }

    @Transactional
    public CustomerContactResponse addContact(Long customerId, CustomerContactRequest req) {
        Customer customer = getOrThrow(customerId);
        CustomerContact contact = customer.addContact(
                req.name(), req.position(), req.phone(), req.email(), req.primary());
        repository.flush(); // 신규 담당자 id 를 응답에 담기 위해 즉시 반영
        return mapper.toContactResponse(contact);
    }

    @Transactional
    public CustomerContactResponse updateContact(Long customerId, Long contactId, CustomerContactRequest req) {
        Customer customer = getOrThrow(customerId);
        CustomerContact contact = customer.updateContact(
                contactId, req.name(), req.position(), req.phone(), req.email(), req.primary());
        return mapper.toContactResponse(contact);
    }

    @Transactional
    public void deleteContact(Long customerId, Long contactId) {
        getOrThrow(customerId).removeContact(contactId);
    }

    private Customer getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: id=" + id));
    }
}
