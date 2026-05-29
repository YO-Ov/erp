package com.hwlee.erp.fi.payment;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PaymentRepository
        extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByNumber(String number);
}
