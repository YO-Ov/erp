package com.hwlee.erp.fi.payment;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.customer.Customer;
import com.hwlee.erp.master.vendor.Vendor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 입금/출금 — OTC 의 마지막 단계 (외상값 결제).
 *
 * <p>{@link PaymentType} 에 따라 거래 상대가 결정된다 — type/party 정합성은 V25 의 CHECK 제약과
 * {@link #receipt}/{@link #disbursement} 팩토리가 함께 보장.
 *
 * <p>학습 1차 범위에선 취소 기능 생략. 추후 도입 시 역분개(차/대 반대 전표) 패턴을 따른다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment")
public class Payment extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30, updatable = false)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16, updatable = false)
    private PaymentType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PaymentStatus status = PaymentStatus.DRAFT;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "description", length = 255)
    private String description;

    public static Payment receipt(String number, Customer customer, BigDecimal amount,
                                  LocalDate paymentDate, String description) {
        if (customer == null) {
            throw new IllegalArgumentException("입금(RECEIPT)에는 customer 가 필수다.");
        }
        Payment p = baseOf(number, amount, paymentDate, description);
        p.type = PaymentType.RECEIPT;
        p.customer = customer;
        return p;
    }

    public static Payment disbursement(String number, Vendor vendor, BigDecimal amount,
                                       LocalDate paymentDate, String description) {
        if (vendor == null) {
            throw new IllegalArgumentException("출금(DISBURSEMENT)에는 vendor 가 필수다.");
        }
        Payment p = baseOf(number, amount, paymentDate, description);
        p.type = PaymentType.DISBURSEMENT;
        p.vendor = vendor;
        return p;
    }

    private static Payment baseOf(String number, BigDecimal amount, LocalDate paymentDate, String description) {
        if (number == null || number.isBlank()) throw new IllegalArgumentException("number 는 비어 있을 수 없다.");
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("amount 는 > 0 이어야 한다.");
        if (paymentDate == null) throw new IllegalArgumentException("paymentDate 는 null 일 수 없다.");
        Payment p = new Payment();
        p.number = number;
        p.amount = amount;
        p.paymentDate = paymentDate;
        p.description = description;
        return p;
    }

    public void post(LocalDateTime now) {
        if (status != PaymentStatus.DRAFT) {
            throw new IllegalStateException("DRAFT Payment 만 확정 가능합니다. 현재: " + status);
        }
        this.status = PaymentStatus.POSTED;
        this.postedAt = now;
    }
}
