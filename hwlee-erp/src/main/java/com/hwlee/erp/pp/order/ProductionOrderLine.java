package com.hwlee.erp.pp.order;

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
 * 생산지시 소요 자재 라인 — BOM×수량으로 굳힌 스냅샷.
 *
 * <p>{@code requiredQty} 는 생성 시점에 고정(이후 BOM 변경과 무관). {@code issuedUnitCost} 는
 * 완료 시 실제 출고된 이동평균 단가가 기록된다(완료 전 NULL).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "production_order_line")
public class ProductionOrderLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    /** 투입 부품 (itemType=COMPONENT). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_item_id", nullable = false)
    private Item component;

    @Column(name = "required_qty", nullable = false, precision = 15, scale = 2)
    private BigDecimal requiredQty;

    /** 완료 시 실제 출고된 이동평균 단가 (완료 전 NULL). */
    @Column(name = "issued_unit_cost", precision = 15, scale = 2)
    private BigDecimal issuedUnitCost;

    ProductionOrderLine(ProductionOrder productionOrder, int lineNo, Item component, BigDecimal requiredQty) {
        if (component == null) throw new IllegalArgumentException("component 는 null 일 수 없다.");
        if (requiredQty == null || requiredQty.signum() <= 0)
            throw new IllegalArgumentException("소요량은 0보다 커야 한다.");
        this.productionOrder = productionOrder;
        this.lineNo = lineNo;
        this.component = component;
        this.requiredQty = requiredQty;
    }

    /** 완료 시 실제 출고 단가 기록 (호출 측 서비스). */
    public void recordIssuedCost(BigDecimal unitCost) {
        this.issuedUnitCost = unitCost;
    }
}
