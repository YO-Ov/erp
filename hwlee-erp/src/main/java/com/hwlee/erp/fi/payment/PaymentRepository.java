package com.hwlee.erp.fi.payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository
        extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByNumber(String number);

    /** Phase 9 — 채권 노령화: 기준일까지 확정(POSTED)된 입금(RECEIPT). 고객별 미수 차감용. */
    @Query("select p from Payment p "
            + "where p.type = com.hwlee.erp.fi.payment.PaymentType.RECEIPT "
            + "and p.status = com.hwlee.erp.fi.payment.PaymentStatus.POSTED "
            + "and p.paymentDate <= :date")
    List<Payment> findPostedReceiptsUpTo(@Param("date") LocalDate date);

    /** 여신 미수금 산정: 한 고객에게서 확정(POSTED)된 입금(RECEIPT) 합계. 발행 인보이스 합계와 차감해 미수금 도출. */
    @Query("select coalesce(sum(p.amount), 0) from Payment p "
            + "where p.customer.id = :customerId "
            + "and p.type = com.hwlee.erp.fi.payment.PaymentType.RECEIPT "
            + "and p.status = com.hwlee.erp.fi.payment.PaymentStatus.POSTED")
    BigDecimal sumPostedReceiptAmountByCustomer(@Param("customerId") Long customerId);
}
