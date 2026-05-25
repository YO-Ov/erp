package com.hwlee.erp.sd.quotation;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.customer.Customer;
import com.hwlee.erp.master.item.Item;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 견적 헤더 — 고객에게 제안한 가격/조건.
 *
 * <p>상태 머신은 도메인 메서드({@link #send()}, {@link #accept()}, {@link #expire()},
 * {@link #cancel()})로만 변경되며, {@code setStatus} 는 노출하지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "quotation")
public class Quotation extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30, updatable = false)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private QuotationStatus status = QuotationStatus.DRAFT;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNo ASC")
    private List<QuotationLine> lines = new ArrayList<>();

    public static Quotation draft(String number, Customer customer, LocalDate issuedDate, LocalDate validUntil) {
        if (number == null || number.isBlank()) throw new IllegalArgumentException("number 는 비어 있을 수 없다.");
        if (customer == null) throw new IllegalArgumentException("customer 는 null 일 수 없다.");
        if (issuedDate == null) throw new IllegalArgumentException("issuedDate 는 null 일 수 없다.");
        if (validUntil != null && validUntil.isBefore(issuedDate))
            throw new IllegalArgumentException("validUntil 은 issuedDate 이후여야 한다.");
        Quotation q = new Quotation();
        q.number = number;
        q.customer = customer;
        q.issuedDate = issuedDate;
        q.validUntil = validUntil;
        return q;
    }

    public QuotationLine addLine(Item item, BigDecimal quantity, BigDecimal unitPrice) {
        ensureEditable();
        QuotationLine line = new QuotationLine(this, lines.size() + 1, item, quantity, unitPrice);
        lines.add(line);
        recalculateTotal();
        return line;
    }

    public void clearLines() {
        ensureEditable();
        lines.clear();
        recalculateTotal();
    }

    public void updateHeader(LocalDate issuedDate, LocalDate validUntil) {
        ensureEditable();
        if (issuedDate == null) throw new IllegalArgumentException("issuedDate 는 null 일 수 없다.");
        if (validUntil != null && validUntil.isBefore(issuedDate))
            throw new IllegalArgumentException("validUntil 은 issuedDate 이후여야 한다.");
        this.issuedDate = issuedDate;
        this.validUntil = validUntil;
    }

    public void send() {
        if (status != QuotationStatus.DRAFT)
            throw new IllegalStateException("DRAFT 견적만 발송 가능합니다. 현재: " + status);
        if (lines.isEmpty())
            throw new IllegalStateException("라인이 비어 있는 견적은 발송할 수 없습니다.");
        this.status = QuotationStatus.SENT;
    }

    public void accept() {
        if (status != QuotationStatus.SENT)
            throw new IllegalStateException("SENT 견적만 수락 처리할 수 있습니다. 현재: " + status);
        this.status = QuotationStatus.ACCEPTED;
    }

    public void expire() {
        if (status != QuotationStatus.SENT)
            throw new IllegalStateException("SENT 견적만 만료 처리할 수 있습니다. 현재: " + status);
        this.status = QuotationStatus.EXPIRED;
    }

    public void cancel() {
        if (status == QuotationStatus.ACCEPTED || status == QuotationStatus.CANCELLED)
            throw new IllegalStateException("이미 수락되었거나 취소된 견적은 취소할 수 없습니다. 현재: " + status);
        this.status = QuotationStatus.CANCELLED;
    }

    public List<QuotationLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    private void ensureEditable() {
        if (status != QuotationStatus.DRAFT)
            throw new IllegalStateException("DRAFT 상태에서만 수정 가능합니다. 현재: " + status);
    }

    private void recalculateTotal() {
        this.totalAmount = lines.stream()
                .map(QuotationLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
