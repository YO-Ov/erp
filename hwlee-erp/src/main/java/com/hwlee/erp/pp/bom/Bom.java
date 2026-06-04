package com.hwlee.erp.pp.bom;

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
 * BOM(Bill of Materials) 한 줄 — "완제품 1개당 이 부품이 몇 개 필요한가" (단일 레벨).
 *
 * <p>Item↔Item 자기참조: {@code product}(완제품, FINISHED) 가 {@code component}(부품, COMPONENT) 를
 * {@code quantity} 개씩 필요로 한다. (product, component) 조합은 유일.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "bom")
public class Bom extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_item_id", nullable = false)
    private Item product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_item_id", nullable = false)
    private Item component;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantity;

    public static Bom of(Item product, Item component, BigDecimal quantity) {
        if (product == null) throw new IllegalArgumentException("product 는 null 일 수 없다.");
        if (component == null) throw new IllegalArgumentException("component 는 null 일 수 없다.");
        if (product.getId() != null && product.getId().equals(component.getId()))
            throw new IllegalArgumentException("완제품과 부품이 같을 수 없다.");
        if (quantity == null || quantity.signum() <= 0)
            throw new IllegalArgumentException("소요량은 0보다 커야 한다.");
        Bom bom = new Bom();
        bom.product = product;
        bom.component = component;
        bom.quantity = quantity;
        return bom;
    }
}
