package com.hwlee.erp.sd.order;

import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class SalesOrderSpecifications {

    private SalesOrderSpecifications() {}

    public static Specification<SalesOrder> customerIdEquals(Long customerId) {
        if (customerId == null) return null;
        return (root, q, cb) -> cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<SalesOrder> salespersonIdEquals(Long salespersonId) {
        if (salespersonId == null) return null;
        return (root, q, cb) -> cb.equal(root.get("salesperson").get("id"), salespersonId);
    }

    public static Specification<SalesOrder> statusEquals(SalesOrderStatus status) {
        if (status == null) return null;
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<SalesOrder> orderedFrom(LocalDate from) {
        if (from == null) return null;
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("orderDate"), from);
    }

    public static Specification<SalesOrder> orderedTo(LocalDate to) {
        if (to == null) return null;
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("orderDate"), to);
    }
}
