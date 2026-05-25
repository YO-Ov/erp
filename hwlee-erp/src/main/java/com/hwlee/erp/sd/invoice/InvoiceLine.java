package com.hwlee.erp.sd.invoice;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.sd.order.SalesOrderLine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "invoice_line")
public class InvoiceLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_line_id", nullable = false)
    private SalesOrderLine salesOrderLine;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    /** SO 라인 단가를 복사 — 미래 단가 변경에도 회계 금액이 보존된다. */
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal;

    InvoiceLine(Invoice invoice, int lineNo, SalesOrderLine salesOrderLine, BigDecimal quantity) {
        if (salesOrderLine == null) throw new IllegalArgumentException("salesOrderLine 은 null 일 수 없다.");
        if (quantity == null || quantity.signum() <= 0)
            throw new IllegalArgumentException("quantity 는 0보다 커야 한다.");
        BigDecimal remaining = salesOrderLine.remainingInvoiceable();
        if (quantity.compareTo(remaining) > 0) {
            throw new IllegalStateException(
                    "잔여 청구 가능량을 초과합니다. remainingInvoiceable=" + remaining + ", quantity=" + quantity);
        }
        this.invoice = invoice;
        this.lineNo = lineNo;
        this.salesOrderLine = salesOrderLine;
        this.quantity = quantity;
        this.unitPrice = salesOrderLine.getUnitPrice();
        this.lineTotal = quantity.multiply(this.unitPrice);
    }
}
