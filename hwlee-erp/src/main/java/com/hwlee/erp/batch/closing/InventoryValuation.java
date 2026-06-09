package com.hwlee.erp.batch.closing;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.mm.warehouse.Warehouse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고 평가 스냅샷 — 평가 기준일의 (품목, 창고)별 평가액.
 *
 * <p>평가액 = {@code qtyOnHand × averageCost} (가중평균법). 그 시점 {@link com.hwlee.erp.mm.stock.Stock}
 * 캐시를 박제한다. (평가일, 품목, 창고) UNIQUE — 재실행 시 평가일 단위로 삭제 후 재삽입.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "inventory_valuation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_inv_val_date_item_wh",
                columnNames = {"valuation_date", "item_id", "warehouse_id"})
)
public class InventoryValuation extends BaseEntity {

    @Column(name = "valuation_date", nullable = false)
    private LocalDate valuationDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "qty_on_hand", nullable = false, precision = 18, scale = 4)
    private BigDecimal qtyOnHand;

    @Column(name = "average_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal averageCost;

    @Column(name = "valuation_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal valuationAmount;

    public static InventoryValuation of(LocalDate valuationDate, Item item, Warehouse warehouse,
                                        BigDecimal qtyOnHand, BigDecimal averageCost) {
        InventoryValuation v = new InventoryValuation();
        v.valuationDate = valuationDate;
        v.item = item;
        v.warehouse = warehouse;
        v.qtyOnHand = qtyOnHand;
        v.averageCost = averageCost;
        v.valuationAmount = qtyOnHand.multiply(averageCost).setScale(2, RoundingMode.HALF_UP);
        return v;
    }
}
