package com.hwlee.erp.mm.purchaseorder;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.vendor.Vendor;
import com.hwlee.erp.mm.warehouse.Warehouse;
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
 * 구매발주(Purchase Order) 헤더 — 거래처에 자재/부품 매입을 발주하는 문서.
 *
 * <p>실무 흐름: 구매 담당이 {@code DRAFT} 로 작성 → 전자결재 상신 → 최종 승인 시
 * {@link #confirm()} 로 {@code CONFIRMED}(발주 확정) → 입고가 진행되어 전량 입고되면
 * {@link #syncReceiptStatus} 로 {@code RECEIVED}(입고 완료) → {@link #close()} 로 마감.
 * 결재 없이는 확정할 수 없다(지출 통제). 입고(GoodsReceipt)는 이 발주를 참조하며, 라인별
 * 입고 누계는 그 참조를 역집계해 계산한다(발주 라인에 동일 품목이 중복되지 않는다는 전제).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "purchase_order")
public class PurchaseOrder extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30, updatable = false)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    /** 입고 예정 창고. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    /** 입고 예정일(희망 납기). 없을 수 있다. */
    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "remark", length = 500)
    private String remark;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNo ASC")
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    public static PurchaseOrder draft(String number, Vendor vendor, Warehouse warehouse,
                                      LocalDate orderDate, LocalDate expectedDate, String remark) {
        if (number == null || number.isBlank()) throw new IllegalArgumentException("number 는 비어 있을 수 없다.");
        if (vendor == null) throw new IllegalArgumentException("vendor 는 null 일 수 없다.");
        if (warehouse == null) throw new IllegalArgumentException("warehouse 는 null 일 수 없다.");
        if (orderDate == null) throw new IllegalArgumentException("orderDate 는 null 일 수 없다.");
        PurchaseOrder po = new PurchaseOrder();
        po.number = number;
        po.vendor = vendor;
        po.warehouse = warehouse;
        po.orderDate = orderDate;
        po.expectedDate = expectedDate;
        po.remark = remark;
        return po;
    }

    public PurchaseOrderLine addLine(Item item, BigDecimal quantity, BigDecimal unitPrice) {
        ensureEditable();
        PurchaseOrderLine line = new PurchaseOrderLine(this, lines.size() + 1, item, quantity, unitPrice);
        lines.add(line);
        return line;
    }

    public void clearLines() {
        ensureEditable();
        lines.clear();
    }

    public void updateHeader(Vendor vendor, Warehouse warehouse, LocalDate orderDate,
                             LocalDate expectedDate, String remark) {
        ensureEditable();
        if (vendor == null) throw new IllegalArgumentException("vendor 는 null 일 수 없다.");
        if (warehouse == null) throw new IllegalArgumentException("warehouse 는 null 일 수 없다.");
        if (orderDate == null) throw new IllegalArgumentException("orderDate 는 null 일 수 없다.");
        this.vendor = vendor;
        this.warehouse = warehouse;
        this.orderDate = orderDate;
        this.expectedDate = expectedDate;
        this.remark = remark;
    }

    /** 발주 합계(공급가) — 전결 금액 판단 기준. */
    public BigDecimal totalAmount() {
        return lines.stream()
                .map(PurchaseOrderLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * DRAFT → CONFIRMED. 전자결재 최종 승인 콜백에서만 호출한다(결재 없이 발주 불가).
     */
    public void confirm() {
        if (status != PurchaseOrderStatus.DRAFT)
            throw new IllegalStateException("DRAFT 발주만 확정 가능합니다. 현재: " + status);
        if (lines.isEmpty())
            throw new IllegalStateException("라인이 비어 있는 발주는 확정할 수 없습니다.");
        this.status = PurchaseOrderStatus.CONFIRMED;
    }

    /**
     * 발주 대비 입고 진행에 따라 CONFIRMED ↔ RECEIVED 를 동기화한다(입고 확정/취소 콜백에서 호출).
     *
     * <ul>
     *   <li>CONFIRMED + 전량 입고 → RECEIVED</li>
     *   <li>RECEIVED + 입고 취소로 미달 → CONFIRMED 로 복귀</li>
     *   <li>그 외(DRAFT/CLOSED/CANCELLED)는 무시 — 이미 종결됐거나 발주 전이라 입고 진행과 무관</li>
     * </ul>
     *
     * @param fullyReceived 발주 전 라인이 전량 입고되었는지(라인별 입고누계 ≥ 발주수량)
     */
    public void syncReceiptStatus(boolean fullyReceived) {
        if (status == PurchaseOrderStatus.CONFIRMED && fullyReceived) {
            this.status = PurchaseOrderStatus.RECEIVED;
        } else if (status == PurchaseOrderStatus.RECEIVED && !fullyReceived) {
            this.status = PurchaseOrderStatus.CONFIRMED;
        }
    }

    /**
     * CONFIRMED/RECEIVED → CLOSED. 발주를 마감한다.
     * 전량 입고(RECEIVED)뿐 아니라 부분 입고 상태(CONFIRMED)에서도 잔량을 포기하고 강제 종결할 수 있다.
     */
    public void close() {
        if (status != PurchaseOrderStatus.CONFIRMED && status != PurchaseOrderStatus.RECEIVED)
            throw new IllegalStateException("확정(CONFIRMED)·입고완료(RECEIVED) 발주만 종료 가능합니다. 현재: " + status);
        this.status = PurchaseOrderStatus.CLOSED;
    }

    /** DRAFT/CONFIRMED → CANCELLED. */
    public void cancel() {
        if (status != PurchaseOrderStatus.DRAFT && status != PurchaseOrderStatus.CONFIRMED)
            throw new IllegalStateException("작성 중/확정 발주만 취소 가능합니다. 현재: " + status);
        this.status = PurchaseOrderStatus.CANCELLED;
    }

    public List<PurchaseOrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    private void ensureEditable() {
        if (status != PurchaseOrderStatus.DRAFT)
            throw new IllegalStateException("DRAFT 상태에서만 수정 가능합니다. 현재: " + status);
    }
}
