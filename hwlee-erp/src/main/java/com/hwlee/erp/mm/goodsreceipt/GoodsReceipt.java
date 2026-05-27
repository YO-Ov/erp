package com.hwlee.erp.mm.goodsreceipt;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 입고 헤더 — 매입처에서 받은 사건의 트랜잭션 기록.
 *
 * <p>{@link #post} 시점에 라인별로 {@code Stock.receive()} (가중평균 갱신) + StockMovement(+) 적재가
 * 한 트랜잭션에서 일어난다. Stock 갱신/Movement 적재는 호출 측 서비스가 수행한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "goods_receipt")
public class GoodsReceipt extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30, updatable = false)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private GoodsReceiptStatus status = GoodsReceiptStatus.DRAFT;

    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNo ASC")
    private List<GoodsReceiptLine> lines = new ArrayList<>();

    public static GoodsReceipt draft(String number, Vendor vendor, Warehouse warehouse, LocalDate receiptDate) {
        if (number == null || number.isBlank()) throw new IllegalArgumentException("number 는 비어 있을 수 없다.");
        if (vendor == null) throw new IllegalArgumentException("vendor 는 null 일 수 없다.");
        if (warehouse == null) throw new IllegalArgumentException("warehouse 는 null 일 수 없다.");
        if (receiptDate == null) throw new IllegalArgumentException("receiptDate 는 null 일 수 없다.");
        GoodsReceipt gr = new GoodsReceipt();
        gr.number = number;
        gr.vendor = vendor;
        gr.warehouse = warehouse;
        gr.receiptDate = receiptDate;
        return gr;
    }

    public GoodsReceiptLine addLine(Item item, BigDecimal quantity, BigDecimal unitCost) {
        ensureEditable();
        GoodsReceiptLine line = new GoodsReceiptLine(this, lines.size() + 1, item, quantity, unitCost);
        lines.add(line);
        return line;
    }

    public void clearLines() {
        ensureEditable();
        lines.clear();
    }

    public void updateHeader(Vendor vendor, Warehouse warehouse, LocalDate receiptDate) {
        ensureEditable();
        if (vendor == null) throw new IllegalArgumentException("vendor 는 null 일 수 없다.");
        if (warehouse == null) throw new IllegalArgumentException("warehouse 는 null 일 수 없다.");
        if (receiptDate == null) throw new IllegalArgumentException("receiptDate 는 null 일 수 없다.");
        this.vendor = vendor;
        this.warehouse = warehouse;
        this.receiptDate = receiptDate;
    }

    /**
     * DRAFT → POSTED. Stock 가중평균 갱신과 StockMovement 적재는 호출 측 서비스가 수행한다.
     */
    public void post(LocalDateTime now) {
        if (status != GoodsReceiptStatus.DRAFT)
            throw new IllegalStateException("DRAFT 입고만 확정 가능합니다. 현재: " + status);
        if (lines.isEmpty())
            throw new IllegalStateException("라인이 비어 있는 입고는 확정할 수 없습니다.");
        this.status = GoodsReceiptStatus.POSTED;
        this.postedAt = now;
    }

    public void cancel() {
        if (status != GoodsReceiptStatus.POSTED)
            throw new IllegalStateException("POSTED 입고만 취소 가능합니다. 현재: " + status);
        this.status = GoodsReceiptStatus.CANCELLED;
    }

    public List<GoodsReceiptLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    private void ensureEditable() {
        if (status != GoodsReceiptStatus.DRAFT)
            throw new IllegalStateException("DRAFT 상태에서만 수정 가능합니다. 현재: " + status);
    }
}
