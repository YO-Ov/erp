package com.hwlee.erp.pp.order;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.item.Item;
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
 * 생산지시 헤더 (Phase 8 PP) — "이 완제품을 이만큼 만들어라".
 *
 * <p>생성 시 완제품의 BOM 을 전개해 소요 자재 라인을 굳힌다(스냅샷). 완료({@link #complete}) 시점에
 * 호출 측 서비스가 부품을 출고(stock.issue)하고 완제품을 입고(stock.receive)한다 — 재고 이동은
 * COMPLETED 전이에서만. 상태머신 {@link ProductionOrderStatus} 참고.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "production_order")
public class ProductionOrder extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30, updatable = false)
    private String number;

    /** 만들 완제품 (itemType=FINISHED). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_item_id", nullable = false)
    private Item product;

    /** 부품 출고·완제품 입고가 일어나는 창고. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ProductionOrderStatus status = ProductionOrderStatus.PLANNED;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** MES 작업지시 전송 추적 (Phase 12). 전송 전이면 null. */
    @Column(name = "mes_work_order_no", length = 30)
    private String mesWorkOrderNo;

    @Column(name = "mes_dispatched_at")
    private LocalDateTime mesDispatchedAt;

    @OneToMany(mappedBy = "productionOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNo ASC")
    private List<ProductionOrderLine> lines = new ArrayList<>();

    public static ProductionOrder draft(String number, Item product, Warehouse warehouse,
                                        BigDecimal quantity, LocalDate orderDate, LocalDate dueDate) {
        if (number == null || number.isBlank()) throw new IllegalArgumentException("number 는 비어 있을 수 없다.");
        if (product == null) throw new IllegalArgumentException("product 는 null 일 수 없다.");
        if (warehouse == null) throw new IllegalArgumentException("warehouse 는 null 일 수 없다.");
        if (quantity == null || quantity.signum() <= 0)
            throw new IllegalArgumentException("생산 수량은 0보다 커야 한다.");
        if (orderDate == null) throw new IllegalArgumentException("orderDate 는 null 일 수 없다.");
        ProductionOrder po = new ProductionOrder();
        po.number = number;
        po.product = product;
        po.warehouse = warehouse;
        po.quantity = quantity;
        po.orderDate = orderDate;
        po.dueDate = dueDate;
        return po;
    }

    /** 소요 자재 라인 추가 (생성 시 BOM×수량 전개로만 호출). */
    public ProductionOrderLine addLine(Item component, BigDecimal requiredQty) {
        if (status != ProductionOrderStatus.PLANNED)
            throw new IllegalStateException("PLANNED 상태에서만 라인 추가 가능합니다. 현재: " + status);
        ProductionOrderLine line = new ProductionOrderLine(this, lines.size() + 1, component, requiredQty);
        lines.add(line);
        return line;
    }

    /** PLANNED → RELEASED. 생산 착수 확정(재고는 아직 미반영). */
    public void release() {
        if (status != ProductionOrderStatus.PLANNED)
            throw new IllegalStateException("PLANNED 생산지시만 착수 가능합니다. 현재: " + status);
        if (lines.isEmpty())
            throw new IllegalStateException("소요 자재가 없는 생산지시는 착수할 수 없습니다(BOM 미등록).");
        this.status = ProductionOrderStatus.RELEASED;
    }

    /** RELEASED → COMPLETED. 부품 출고·완제품 입고는 호출 측 서비스가 수행한다. */
    public void complete(LocalDateTime now) {
        if (status != ProductionOrderStatus.RELEASED)
            throw new IllegalStateException("RELEASED 생산지시만 완료 가능합니다. 현재: " + status);
        this.status = ProductionOrderStatus.COMPLETED;
        this.completedAt = now;
    }

    public void cancel() {
        if (status == ProductionOrderStatus.COMPLETED || status == ProductionOrderStatus.CANCELLED)
            throw new IllegalStateException("이미 종료된 생산지시는 취소할 수 없습니다. 현재: " + status);
        // MES 로 전송된 생산지시는 함부로 취소 못 한다 — 현장 작업지시가 이미 진행 중일 수 있어
        // ERP 만 취소하면 MES 와 불일치한다. SD 의 "출하 실적 있으면 수주 취소 거부" 와 같은 사상.
        if (mesWorkOrderNo != null)
            throw new IllegalStateException(
                    "MES 로 전송된 생산지시는 취소할 수 없습니다 (MES 작업지시 " + mesWorkOrderNo
                            + " 진행 중). MES 현장에서 먼저 처리해야 합니다.");
        this.status = ProductionOrderStatus.CANCELLED;
    }

    /** MES 작업지시 전송 결과 기록 (Phase 12). 멱등 재전송 시 같은 값으로 덮어써도 무방. */
    public void markDispatched(String mesWorkOrderNo, LocalDateTime now) {
        this.mesWorkOrderNo = mesWorkOrderNo;
        this.mesDispatchedAt = now;
    }

    public List<ProductionOrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
