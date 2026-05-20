package com.hwlee.erp.master.customer;

import com.hwlee.erp.common.code.CodeGenerator;
import com.hwlee.erp.master.customer.dto.CustomerCreateRequest;
import com.hwlee.erp.master.customer.dto.CustomerResponse;
import com.hwlee.erp.master.customer.dto.CustomerUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
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
        Customer customer = Customer.create(
                code,
                req.name(),
                req.businessNo(),
                req.address(),
                req.creditLimit(),
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
        customer.update(req.name(), req.address(), req.creditLimit(), req.paymentTerms());
        return mapper.toResponse(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = getOrThrow(id);
        repository.delete(customer); // @SQLDelete 에 의해 Soft Delete 로 동작
    }

    private Customer getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: id=" + id));
    }
}
