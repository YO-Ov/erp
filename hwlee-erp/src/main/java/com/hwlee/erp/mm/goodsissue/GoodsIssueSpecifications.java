package com.hwlee.erp.mm.goodsissue;

import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class GoodsIssueSpecifications {

    private GoodsIssueSpecifications() {}

    public static Specification<GoodsIssue> warehouseIdEquals(Long id) {
        if (id == null) return null;
        return (root, q, cb) -> cb.equal(root.get("warehouse").get("id"), id);
    }

    public static Specification<GoodsIssue> statusEquals(GoodsIssueStatus status) {
        if (status == null) return null;
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<GoodsIssue> reasonEquals(GoodsIssueReason reason) {
        if (reason == null) return null;
        return (root, q, cb) -> cb.equal(root.get("reason"), reason);
    }

    public static Specification<GoodsIssue> issueFrom(LocalDate from) {
        if (from == null) return null;
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("issueDate"), from);
    }

    public static Specification<GoodsIssue> issueTo(LocalDate to) {
        if (to == null) return null;
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("issueDate"), to);
    }
}
