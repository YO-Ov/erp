package com.hwlee.erp.sd.delivery;

import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class DeliverySpecifications {

    private DeliverySpecifications() {}

    public static Specification<Delivery> salesOrderIdEquals(Long id) {
        if (id == null) return null;
        return (root, q, cb) -> cb.equal(root.get("salesOrder").get("id"), id);
    }

    public static Specification<Delivery> statusEquals(DeliveryStatus status) {
        if (status == null) return null;
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Delivery> shippedFrom(LocalDate from) {
        if (from == null) return null;
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("shippedDate"), from);
    }

    public static Specification<Delivery> shippedTo(LocalDate to) {
        if (to == null) return null;
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("shippedDate"), to);
    }
}
