package com.hwlee.erp.sd.invoice;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.sd.order.SalesOrder;
import com.hwlee.erp.sd.order.SalesOrderLine;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인보이스 헤더 — 한국 부가세 10% 단일 정책.
 *
 * <p>금액 계산:
 * <ul>
 *   <li>{@code subtotal = SUM(line_total)} — 공급가 합계</li>
 *   <li>{@code tax_amount = subtotal × 0.10}</li>
 *   <li>{@code total_amount = subtotal + tax_amount}</li>
 * </ul>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "invoice")
public class Invoice extends BaseEntity {

    public static final BigDecimal VAT_RATE = new BigDecimal("0.10");

    @Column(name = "number", nullable = false, unique = true, length = 30, updatable = false)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNo ASC")
    private List<InvoiceLine> lines = new ArrayList<>();

    public static Invoice draft(String number, SalesOrder salesOrder, LocalDate invoiceDate) {
        if (number == null || number.isBlank()) throw new IllegalArgumentException("number 는 비어 있을 수 없다.");
        if (salesOrder == null) throw new IllegalArgumentException("salesOrder 는 null 일 수 없다.");
        if (invoiceDate == null) throw new IllegalArgumentException("invoiceDate 는 null 일 수 없다.");
        Invoice inv = new Invoice();
        inv.number = number;
        inv.salesOrder = salesOrder;
        inv.invoiceDate = invoiceDate;
        return inv;
    }

    public InvoiceLine addLine(SalesOrderLine salesOrderLine, BigDecimal quantity) {
        if (status != InvoiceStatus.DRAFT)
            throw new IllegalStateException("DRAFT 인보이스만 라인 추가 가능합니다. 현재: " + status);
        InvoiceLine line = new InvoiceLine(this, lines.size() + 1, salesOrderLine, quantity);
        lines.add(line);
        recalculateTotals();
        return line;
    }

    /**
     * 인보이스 확정. SO 라인의 {@code invoiced_qty} 누적과 SO 헤더 상태 전이는 호출 측 서비스에서 수행한다.
     */
    public void issue() {
        if (status != InvoiceStatus.DRAFT)
            throw new IllegalStateException("DRAFT 인보이스만 발행 가능합니다. 현재: " + status);
        if (lines.isEmpty())
            throw new IllegalStateException("라인이 비어 있는 인보이스는 발행할 수 없습니다.");
        this.status = InvoiceStatus.ISSUED;
    }

    public void cancel() {
        if (status != InvoiceStatus.ISSUED)
            throw new IllegalStateException("ISSUED 인보이스만 취소 가능합니다. 현재: " + status);
        this.status = InvoiceStatus.CANCELLED;
    }

    public List<InvoiceLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    private void recalculateTotals() {
        this.subtotal = lines.stream()
                .map(InvoiceLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.taxAmount = subtotal.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        this.totalAmount = subtotal.add(taxAmount);
    }
}
