package com.hwlee.erp.mm.goodsreceipt;

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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "goods_receipt_line")
public class GoodsReceiptLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal;

    GoodsReceiptLine(GoodsReceipt goodsReceipt, int lineNo, Item item, BigDecimal quantity, BigDecimal unitCost) {
        if (item == null) throw new IllegalArgumentException("item 은 null 일 수 없다.");
        if (quantity == null || quantity.signum() <= 0)
            throw new IllegalArgumentException("quantity 는 0보다 커야 한다.");
        if (unitCost == null || unitCost.signum() < 0)
            throw new IllegalArgumentException("unitCost 는 0 이상이어야 한다.");
        this.goodsReceipt = goodsReceipt;
        this.lineNo = lineNo;
        this.item = item;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.lineTotal = quantity.multiply(unitCost);
    }
}
