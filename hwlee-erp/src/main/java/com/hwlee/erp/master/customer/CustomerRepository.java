package com.hwlee.erp.master.customer;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerRepository
        extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByCode(String code);

    boolean existsByBusinessNo(String businessNo);

    Page<Customer> findAll(org.springframework.data.jpa.domain.Specification<Customer> spec, Pageable pageable);
}
