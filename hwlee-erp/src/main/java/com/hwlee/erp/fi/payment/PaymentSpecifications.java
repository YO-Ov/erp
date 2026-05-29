package com.hwlee.erp.fi.payment;

import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class PaymentSpecifications {

    private PaymentSpecifications() {}

    public static Specification<Payment> typeEquals(PaymentType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Payment> customerIdEquals(Long customerId) {
        return (root, query, cb) -> customerId == null ? null : cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Payment> vendorIdEquals(Long vendorId) {
        return (root, query, cb) -> vendorId == null ? null : cb.equal(root.get("vendor").get("id"), vendorId);
    }

    public static Specification<Payment> dateFrom(LocalDate from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("paymentDate"), from);
    }

    public static Specification<Payment> dateTo(LocalDate to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("paymentDate"), to);
    }
}
