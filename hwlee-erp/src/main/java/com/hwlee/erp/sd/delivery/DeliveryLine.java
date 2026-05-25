package com.hwlee.erp.sd.delivery;

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
@Table(name = "delivery_line")
public class DeliveryLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_line_id", nullable = false)
    private SalesOrderLine salesOrderLine;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    DeliveryLine(Delivery delivery, int lineNo, SalesOrderLine salesOrderLine, BigDecimal quantity) {
        if (salesOrderLine == null) throw new IllegalArgumentException("salesOrderLine 은 null 일 수 없다.");
        if (quantity == null || quantity.signum() <= 0)
            throw new IllegalArgumentException("quantity 는 0보다 커야 한다.");
        BigDecimal remaining = salesOrderLine.remainingShippable();
        if (quantity.compareTo(remaining) > 0) {
            throw new IllegalStateException(
                    "잔여 출하 가능량을 초과합니다. remainingShippable=" + remaining + ", quantity=" + quantity);
        }
        this.delivery = delivery;
        this.lineNo = lineNo;
        this.salesOrderLine = salesOrderLine;
        this.quantity = quantity;
    }
}
