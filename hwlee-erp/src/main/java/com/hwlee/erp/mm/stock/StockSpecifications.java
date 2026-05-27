package com.hwlee.erp.mm.stock;

import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;

public final class StockSpecifications {

    private StockSpecifications() {}

    public static Specification<Stock> itemIdEquals(Long id) {
        if (id == null) return null;
        return (root, q, cb) -> cb.equal(root.get("item").get("id"), id);
    }

    public static Specification<Stock> warehouseIdEquals(Long id) {
        if (id == null) return null;
        return (root, q, cb) -> cb.equal(root.get("warehouse").get("id"), id);
    }

    public static Specification<Stock> qtyGreaterThan(BigDecimal qty) {
        if (qty == null) return null;
        return (root, q, cb) -> cb.greaterThan(root.get("qtyOnHand"), qty);
    }
}
