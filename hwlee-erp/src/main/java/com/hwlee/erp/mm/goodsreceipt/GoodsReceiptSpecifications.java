package com.hwlee.erp.mm.goodsreceipt;

import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class GoodsReceiptSpecifications {

    private GoodsReceiptSpecifications() {}

    public static Specification<GoodsReceipt> vendorIdEquals(Long id) {
        if (id == null) return null;
        return (root, q, cb) -> cb.equal(root.get("vendor").get("id"), id);
    }

    public static Specification<GoodsReceipt> warehouseIdEquals(Long id) {
        if (id == null) return null;
        return (root, q, cb) -> cb.equal(root.get("warehouse").get("id"), id);
    }

    public static Specification<GoodsReceipt> statusEquals(GoodsReceiptStatus status) {
        if (status == null) return null;
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<GoodsReceipt> receiptFrom(LocalDate from) {
        if (from == null) return null;
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("receiptDate"), from);
    }

    public static Specification<GoodsReceipt> receiptTo(LocalDate to) {
        if (to == null) return null;
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("receiptDate"), to);
    }
}
