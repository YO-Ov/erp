package com.hwlee.erp.mm.purchaseorder;

import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class PurchaseOrderSpecifications {

    private PurchaseOrderSpecifications() {}

    public static Specification<PurchaseOrder> vendorIdEquals(Long id) {
        if (id == null) return null;
        return (root, q, cb) -> cb.equal(root.get("vendor").get("id"), id);
    }

    public static Specification<PurchaseOrder> warehouseIdEquals(Long id) {
        if (id == null) return null;
        return (root, q, cb) -> cb.equal(root.get("warehouse").get("id"), id);
    }

    public static Specification<PurchaseOrder> statusEquals(PurchaseOrderStatus status) {
        if (status == null) return null;
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<PurchaseOrder> orderFrom(LocalDate from) {
        if (from == null) return null;
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("orderDate"), from);
    }

    public static Specification<PurchaseOrder> orderTo(LocalDate to) {
        if (to == null) return null;
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("orderDate"), to);
    }
}
