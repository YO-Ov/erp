package com.hwlee.erp.sd.invoice;

import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class InvoiceSpecifications {

    private InvoiceSpecifications() {}

    public static Specification<Invoice> salesOrderIdEquals(Long id) {
        if (id == null) return null;
        return (root, q, cb) -> cb.equal(root.get("salesOrder").get("id"), id);
    }

    public static Specification<Invoice> statusEquals(InvoiceStatus status) {
        if (status == null) return null;
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Invoice> issuedFrom(LocalDate from) {
        if (from == null) return null;
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("invoiceDate"), from);
    }

    public static Specification<Invoice> issuedTo(LocalDate to) {
        if (to == null) return null;
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("invoiceDate"), to);
    }
}
