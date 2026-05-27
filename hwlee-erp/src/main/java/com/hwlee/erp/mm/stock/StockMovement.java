package com.hwlee.erp.mm.stock;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.mm.warehouse.Warehouse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고 이동 원장 — append-only.
 *
 * <p>한 번 저장되면 절대 수정/삭제되지 않는다. setter 없음.
 *
 * <p>{@code qty_delta} 의 부호 하나로 입고/출고/조정 모두를 표현한다 — 추상화의 힘.
 * 새 이동 유형이 추가돼도 테이블 구조는 안 바꾼다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "stock_movement")
public class StockMovement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false, updatable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false, updatable = false)
    private Warehouse warehouse;

    @Column(name = "qty_delta", nullable = false, updatable = false, precision = 18, scale = 4)
    private BigDecimal qtyDelta;

    @Column(name = "unit_cost", nullable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal unitCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, updatable = false, length = 20)
    private MovementReason reason;

    @Column(name = "ref_type", updatable = false, length = 10)
    private String refType;

    @Column(name = "ref_id", updatable = false)
    private Long refId;

    @Column(name = "moved_at", nullable = false, updatable = false)
    private LocalDateTime movedAt;

    public static StockMovement of(Item item, Warehouse warehouse, BigDecimal qtyDelta,
                                   BigDecimal unitCost, MovementReason reason,
                                   String refType, Long refId, LocalDateTime movedAt) {
        if (item == null) throw new IllegalArgumentException("item 은 null 일 수 없다.");
        if (warehouse == null) throw new IllegalArgumentException("warehouse 는 null 일 수 없다.");
        if (qtyDelta == null || qtyDelta.signum() == 0)
            throw new IllegalArgumentException("qtyDelta 는 0이 될 수 없다.");
        if (unitCost == null || unitCost.signum() < 0)
            throw new IllegalArgumentException("unitCost 는 0 이상이어야 한다.");
        if (reason == null) throw new IllegalArgumentException("reason 은 null 일 수 없다.");
        if (movedAt == null) throw new IllegalArgumentException("movedAt 은 null 일 수 없다.");
        if (reason.isPositive() && qtyDelta.signum() < 0)
            throw new IllegalArgumentException(
                    "reason " + reason + " 은 양수 qtyDelta 여야 한다: " + qtyDelta);
        if (reason.isNegative() && qtyDelta.signum() > 0)
            throw new IllegalArgumentException(
                    "reason " + reason + " 은 음수 qtyDelta 여야 한다: " + qtyDelta);

        StockMovement m = new StockMovement();
        m.item = item;
        m.warehouse = warehouse;
        m.qtyDelta = qtyDelta;
        m.unitCost = unitCost;
        m.reason = reason;
        m.refType = refType;
        m.refId = refId;
        m.movedAt = movedAt;
        return m;
    }
}
