package com.hwlee.erp.sd.delivery;

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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 출하 헤더 — 한 출하는 한 수주에 속하며, 라인 단위로 SO 라인의 잔여 출하 가능량을 차감한다.
 *
 * <p>상태 전이는 {@link #ship()} / {@link #cancel()} 도메인 메서드로만.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "delivery")
public class Delivery extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30, updatable = false)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private DeliveryStatus status = DeliveryStatus.DRAFT;

    @Column(name = "shipped_date", nullable = false)
    private LocalDate shippedDate;

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNo ASC")
    private List<DeliveryLine> lines = new ArrayList<>();

    public static Delivery draft(String number, SalesOrder salesOrder, LocalDate shippedDate) {
        if (number == null || number.isBlank()) throw new IllegalArgumentException("number 는 비어 있을 수 없다.");
        if (salesOrder == null) throw new IllegalArgumentException("salesOrder 는 null 일 수 없다.");
        if (shippedDate == null) throw new IllegalArgumentException("shippedDate 는 null 일 수 없다.");
        Delivery d = new Delivery();
        d.number = number;
        d.salesOrder = salesOrder;
        d.shippedDate = shippedDate;
        return d;
    }

    public DeliveryLine addLine(SalesOrderLine salesOrderLine, BigDecimal quantity) {
        if (status != DeliveryStatus.DRAFT)
            throw new IllegalStateException("DRAFT 출하만 라인 추가 가능합니다. 현재: " + status);
        DeliveryLine line = new DeliveryLine(this, lines.size() + 1, salesOrderLine, quantity);
        lines.add(line);
        return line;
    }

    /**
     * 출하 확정. SO 라인의 {@code shipped_qty} 누적과 SO 헤더 상태 전이는 호출 측 서비스에서 수행한다.
     */
    public void ship() {
        if (status != DeliveryStatus.DRAFT)
            throw new IllegalStateException("DRAFT 출하만 확정 가능합니다. 현재: " + status);
        if (lines.isEmpty())
            throw new IllegalStateException("라인이 비어 있는 출하는 확정할 수 없습니다.");
        this.status = DeliveryStatus.SHIPPED;
    }

    public void cancel() {
        if (status != DeliveryStatus.SHIPPED)
            throw new IllegalStateException("SHIPPED 출하만 취소 가능합니다. 현재: " + status);
        this.status = DeliveryStatus.CANCELLED;
    }

    public List<DeliveryLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
