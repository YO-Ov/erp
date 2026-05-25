package com.hwlee.erp.sd.quotation;

import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class QuotationSpecifications {

    private QuotationSpecifications() {}

    public static Specification<Quotation> customerIdEquals(Long customerId) {
        if (customerId == null) return null;
        return (root, q, cb) -> cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Quotation> statusEquals(QuotationStatus status) {
        if (status == null) return null;
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Quotation> issuedFrom(LocalDate from) {
        if (from == null) return null;
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("issuedDate"), from);
    }

    public static Specification<Quotation> issuedTo(LocalDate to) {
        if (to == null) return null;
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("issuedDate"), to);
    }
}
