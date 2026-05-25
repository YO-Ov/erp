package com.hwlee.erp.sd.order;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.item.Item;
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

/**
 * 수주 라인 — 부분 출하/청구의 단일 진실.
 *
 * <p>{@code shipped_qty} 와 {@code invoiced_qty} 는 누적되며, 다음 불변 조건을 만족해야 한다:
 * <ul>
 *     <li>{@code shipped_qty <= order_qty}</li>
 *     <li>{@code invoiced_qty <= shipped_qty}</li>
 * </ul>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "sales_order_line")
public class SalesOrderLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "order_qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal orderQty;

    @Column(name = "shipped_qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal shippedQty = BigDecimal.ZERO;

    @Column(name = "invoiced_qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal invoicedQty = BigDecimal.ZERO;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal;

    SalesOrderLine(SalesOrder salesOrder, int lineNo, Item item, BigDecimal orderQty, BigDecimal unitPrice) {
        if (item == null) throw new IllegalArgumentException("item 은 null 일 수 없다.");
        if (orderQty == null || orderQty.signum() <= 0)
            throw new IllegalArgumentException("orderQty 는 0보다 커야 한다.");
        if (unitPrice == null || unitPrice.signum() < 0)
            throw new IllegalArgumentException("unitPrice 는 0 이상이어야 한다.");
        this.salesOrder = salesOrder;
        this.lineNo = lineNo;
        this.item = item;
        this.orderQty = orderQty;
        this.unitPrice = unitPrice;
        this.lineTotal = orderQty.multiply(unitPrice);
    }

    void addShippedQty(BigDecimal qty) {
        if (qty == null || qty.signum() <= 0)
            throw new IllegalArgumentException("출하 수량은 0보다 커야 한다.");
        BigDecimal next = this.shippedQty.add(qty);
        if (next.compareTo(orderQty) > 0)
            throw new IllegalStateException(
                    "출하 누적이 주문량을 초과합니다. orderQty=" + orderQty + ", shippedQty(after)=" + next);
        this.shippedQty = next;
    }

    void subtractShippedQty(BigDecimal qty) {
        if (qty == null || qty.signum() <= 0)
            throw new IllegalArgumentException("취소 수량은 0보다 커야 한다.");
        BigDecimal next = this.shippedQty.subtract(qty);
        if (next.signum() < 0)
            throw new IllegalStateException("출하 누적이 음수가 됩니다. shippedQty=" + shippedQty + ", 차감=" + qty);
        if (next.compareTo(invoicedQty) < 0)
            throw new IllegalStateException(
                    "이미 청구된 수량보다 적게 출하 누적을 되돌릴 수 없습니다. invoicedQty=" + invoicedQty
                            + ", shippedQty(after)=" + next);
        this.shippedQty = next;
    }

    void addInvoicedQty(BigDecimal qty) {
        if (qty == null || qty.signum() <= 0)
            throw new IllegalArgumentException("청구 수량은 0보다 커야 한다.");
        BigDecimal next = this.invoicedQty.add(qty);
        if (next.compareTo(shippedQty) > 0)
            throw new IllegalStateException(
                    "청구 누적이 출하 누적을 초과합니다. shippedQty=" + shippedQty + ", invoicedQty(after)=" + next);
        this.invoicedQty = next;
    }

    void subtractInvoicedQty(BigDecimal qty) {
        if (qty == null || qty.signum() <= 0)
            throw new IllegalArgumentException("취소 수량은 0보다 커야 한다.");
        BigDecimal next = this.invoicedQty.subtract(qty);
        if (next.signum() < 0)
            throw new IllegalStateException("청구 누적이 음수가 됩니다. invoicedQty=" + invoicedQty + ", 차감=" + qty);
        this.invoicedQty = next;
    }

    public BigDecimal remainingShippable() {
        return orderQty.subtract(shippedQty);
    }

    public BigDecimal remainingInvoiceable() {
        return shippedQty.subtract(invoicedQty);
    }

    public boolean isFullyShipped() {
        return shippedQty.compareTo(orderQty) == 0;
    }

    public boolean isFullyInvoiced() {
        return invoicedQty.compareTo(orderQty) == 0;
    }
}
