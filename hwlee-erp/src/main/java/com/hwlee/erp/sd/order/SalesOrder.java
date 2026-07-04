package com.hwlee.erp.sd.order;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.customer.Customer;
import com.hwlee.erp.master.employee.Employee;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.sd.quotation.Quotation;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수주 헤더 — OTC 흐름의 중심축.
 *
 * <p>출하/청구 라인은 모두 {@link SalesOrderLine} 을 직접 가리키고,
 * 누적 수량은 라인의 {@code shipped_qty / invoiced_qty} 에서 단일 진실로 관리된다.
 *
 * <p>신용한도 검증은 외부 의존(다른 SO 합계 조회)이라 서비스 단에서 처리하고,
 * {@link #confirm()} 자체는 자기 상태만 검증한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "sales_order")
public class SalesOrder extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30, updatable = false)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salesperson_id")
    private Employee salesperson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id")
    private Quotation quotation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SalesOrderStatus status = SalesOrderStatus.DRAFT;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNo ASC")
    private List<SalesOrderLine> lines = new ArrayList<>();

    public static SalesOrder draft(String number, Customer customer, Employee salesperson,
                                   Quotation quotation, LocalDate orderDate) {
        if (number == null || number.isBlank()) throw new IllegalArgumentException("number 는 비어 있을 수 없다.");
        if (customer == null) throw new IllegalArgumentException("customer 는 null 일 수 없다.");
        if (orderDate == null) throw new IllegalArgumentException("orderDate 는 null 일 수 없다.");
        SalesOrder so = new SalesOrder();
        so.number = number;
        so.customer = customer;
        so.salesperson = salesperson;
        so.quotation = quotation;
        so.orderDate = orderDate;
        return so;
    }

    public SalesOrderLine addLine(Item item, BigDecimal qty, BigDecimal unitPrice) {
        ensureEditable();
        SalesOrderLine line = new SalesOrderLine(this, lines.size() + 1, item, qty, unitPrice);
        lines.add(line);
        recalculateTotal();
        return line;
    }

    public void clearLines() {
        ensureEditable();
        lines.clear();
        recalculateTotal();
    }

    public void updateHeader(Employee salesperson, LocalDate orderDate) {
        ensureEditable();
        if (orderDate == null) throw new IllegalArgumentException("orderDate 는 null 일 수 없다.");
        this.salesperson = salesperson;
        this.orderDate = orderDate;
    }

    /**
     * DRAFT → CONFIRMED. 신용한도/고객 상태 검증은 서비스가 호출 직전에 수행한다.
     */
    public void confirm(LocalDateTime now) {
        if (status != SalesOrderStatus.DRAFT)
            throw new IllegalStateException("DRAFT 수주만 확정 가능합니다. 현재: " + status);
        if (lines.isEmpty())
            throw new IllegalStateException("라인이 비어 있는 수주는 확정할 수 없습니다.");
        this.status = SalesOrderStatus.CONFIRMED;
        this.confirmedAt = now;
    }

    public void cancel() {
        if (status != SalesOrderStatus.DRAFT && status != SalesOrderStatus.CONFIRMED)
            throw new IllegalStateException(
                    "DRAFT 또는 CONFIRMED 수주만 취소 가능합니다. 현재: " + status);
        boolean anyShipped = lines.stream().anyMatch(l -> l.getShippedQty().signum() > 0);
        if (anyShipped)
            throw new IllegalStateException("출하 실적이 있는 수주는 취소할 수 없습니다.");
        this.status = SalesOrderStatus.CANCELLED;
    }

    /**
     * INVOICED → CLOSED. 전량 출하·청구가 끝난 수주를 담당자가 명시적으로 마감한다.
     *
     * <p>수금 여부와는 무관하다 — 실제 입금은 FI 의 Payment(RECEIPT) 소관이며,
     * 이 마감은 "물건 다 보내고 세금계산서도 다 끊었으니 영업상 종료" 라는 의미다.
     * CLOSED 는 {@link #recomputeStatus()} 재계산에서 제외되어 이후 동결된다.
     */
    public void close() {
        if (status != SalesOrderStatus.INVOICED)
            throw new IllegalStateException("INVOICED(전량 청구 완료) 수주만 종료 가능합니다. 현재: " + status);
        this.status = SalesOrderStatus.CLOSED;
    }

    /**
     * 출하 라인 한 건의 수량을 SO 라인에 누적하고, 헤더 상태를 라인 누적값에서 다시 계산한다.
     * 호출 측: Delivery 가 SHIPPED 로 확정되는 시점.
     */
    public void recordShipment(SalesOrderLine line, BigDecimal qty) {
        ensureLineBelongsToThis(line);
        ensureProgressable("출하");
        line.addShippedQty(qty);
        recomputeStatus();
    }

    /**
     * 출하 취소 — SO 라인의 shipped_qty 를 감소시키고 헤더 상태를 라인 누적값에서 다시 계산한다.
     */
    public void cancelShipment(SalesOrderLine line, BigDecimal qty) {
        ensureLineBelongsToThis(line);
        ensureProgressable("출하 취소");
        line.subtractShippedQty(qty);
        recomputeStatus();
    }

    /**
     * 인보이스 라인 한 건의 수량을 SO 라인에 누적하고, 헤더 상태를 라인 누적값에서 다시 계산한다.
     * 호출 측: Invoice 가 ISSUED 로 확정되는 시점.
     */
    public void recordInvoicing(SalesOrderLine line, BigDecimal qty) {
        ensureLineBelongsToThis(line);
        ensureProgressable("청구");
        line.addInvoicedQty(qty);
        recomputeStatus();
    }

    public void cancelInvoicing(SalesOrderLine line, BigDecimal qty) {
        ensureLineBelongsToThis(line);
        ensureProgressable("청구 취소");
        line.subtractInvoicedQty(qty);
        recomputeStatus();
    }

    public List<SalesOrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public SalesOrderLine findLineById(Long lineId) {
        return lines.stream()
                .filter(l -> l.getId() != null && l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "이 수주에 해당 라인이 없습니다. salesOrderLineId=" + lineId));
    }

    private boolean isAllShipped() {
        return !lines.isEmpty() && lines.stream().allMatch(SalesOrderLine::isFullyShipped);
    }

    private boolean isAllInvoiced() {
        return !lines.isEmpty() && lines.stream().allMatch(SalesOrderLine::isFullyInvoiced);
    }

    /**
     * 라인 누적값(shipped/invoiced)으로부터 헤더 상태를 재계산한다.
     * <p>전이 가능한 상태(CONFIRMED ~ INVOICED) 안에서만 동작 — DRAFT/CANCELLED/CLOSED 는 그대로 둔다.
     */
    private void recomputeStatus() {
        if (status == SalesOrderStatus.DRAFT
                || status == SalesOrderStatus.CANCELLED
                || status == SalesOrderStatus.CLOSED) {
            return;
        }
        boolean anyShipped = lines.stream().anyMatch(l -> l.getShippedQty().signum() > 0);
        boolean anyInvoiced = lines.stream().anyMatch(l -> l.getInvoicedQty().signum() > 0);
        if (isAllInvoiced()) {
            this.status = SalesOrderStatus.INVOICED;
        } else if (anyInvoiced) {
            this.status = SalesOrderStatus.INVOICING;
        } else if (isAllShipped()) {
            this.status = SalesOrderStatus.SHIPPED;
        } else if (anyShipped) {
            this.status = SalesOrderStatus.SHIPPING;
        } else {
            this.status = SalesOrderStatus.CONFIRMED;
        }
    }

    private void ensureEditable() {
        if (status != SalesOrderStatus.DRAFT)
            throw new IllegalStateException("DRAFT 상태에서만 수정 가능합니다. 현재: " + status);
    }

    /**
     * 출하/청구/취소 가능 상태인지 확인.
     * CANCELLED/DRAFT/CLOSED 는 모두 거부.
     */
    private void ensureProgressable(String op) {
        if (status == SalesOrderStatus.DRAFT
                || status == SalesOrderStatus.CANCELLED
                || status == SalesOrderStatus.CLOSED) {
            throw new IllegalStateException(op + " 가능 상태가 아닙니다. 현재: " + status);
        }
    }

    /**
     * 라인이 이 수주에 속하는지 확인.
     * Hibernate 프록시일 수 있으므로 ID 기반으로 비교 (ID 가 아직 없는 영속화 전엔 reference 비교).
     */
    private void ensureLineBelongsToThis(SalesOrderLine line) {
        if (line == null) throw new IllegalArgumentException("line 은 null 일 수 없다.");
        SalesOrder owner = line.getSalesOrder();
        if (owner == null) throw new IllegalArgumentException("이 수주에 속하지 않는 라인입니다.");
        if (this.getId() != null) {
            if (!this.getId().equals(owner.getId())) {
                throw new IllegalArgumentException("이 수주에 속하지 않는 라인입니다.");
            }
        } else if (owner != this) {
            throw new IllegalArgumentException("이 수주에 속하지 않는 라인입니다.");
        }
    }

    private void recalculateTotal() {
        this.totalAmount = lines.stream()
                .map(SalesOrderLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
