package com.hwlee.erp.master.item;

import com.hwlee.erp.common.entity.BaseEntityWithCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * 상품 마스터 — 영업/구매/재고/생산 모두가 참조하는 ERP 의 "혈관".
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "item")
@SQLDelete(sql = "UPDATE item SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Item extends BaseEntityWithCode {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private ItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 10)
    private ItemUnit unit;

    @Column(name = "standard_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal standardCost;

    @Column(name = "standard_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal standardPrice;

    public static Item create(String code, String name, ItemCategory category, ItemUnit unit,
                              BigDecimal standardCost, BigDecimal standardPrice) {
        validate(name, category, unit, standardCost, standardPrice);
        Item item = new Item();
        item.assignCode(code);
        item.name = name;
        item.category = category;
        item.unit = unit;
        item.standardCost = standardCost;
        item.standardPrice = standardPrice;
        return item;
    }

    public void update(String name, ItemCategory category, ItemUnit unit,
                       BigDecimal standardCost, BigDecimal standardPrice) {
        validate(name, category, unit, standardCost, standardPrice);
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.standardCost = standardCost;
        this.standardPrice = standardPrice;
    }

    private static void validate(String name, ItemCategory category, ItemUnit unit,
                                 BigDecimal standardCost, BigDecimal standardPrice) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 은 비어 있을 수 없다.");
        }
        if (category == null) {
            throw new IllegalArgumentException("category 는 null 일 수 없다.");
        }
        if (unit == null) {
            throw new IllegalArgumentException("unit 은 null 일 수 없다.");
        }
        if (standardCost == null || standardCost.signum() < 0) {
            throw new IllegalArgumentException("standardCost 는 0 이상이어야 한다.");
        }
        if (standardPrice == null || standardPrice.signum() < 0) {
            throw new IllegalArgumentException("standardPrice 는 0 이상이어야 한다.");
        }
        // 비즈니스 규칙: standardPrice >= standardCost 는 Phase 5 에서 정책 결정.
        // 실무에서는 손해 판매도 가능하므로 일단 검증하지 않는다.
    }
}
