package com.hwlee.erp.mm.stock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.data.jpa.domain.Specification;

public final class StockMovementSpecifications {

    private StockMovementSpecifications() {}

    public static Specification<StockMovement> itemIdEquals(Long id) {
        if (id == null) return null;
        return (root, q, cb) -> cb.equal(root.get("item").get("id"), id);
    }

    public static Specification<StockMovement> warehouseIdEquals(Long id) {
        if (id == null) return null;
        return (root, q, cb) -> cb.equal(root.get("warehouse").get("id"), id);
    }

    public static Specification<StockMovement> reasonEquals(MovementReason reason) {
        if (reason == null) return null;
        return (root, q, cb) -> cb.equal(root.get("reason"), reason);
    }

    public static Specification<StockMovement> movedFrom(LocalDate from) {
        if (from == null) return null;
        LocalDateTime start = LocalDateTime.of(from, LocalTime.MIN);
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("movedAt"), start);
    }

    public static Specification<StockMovement> movedTo(LocalDate to) {
        if (to == null) return null;
        LocalDateTime end = LocalDateTime.of(to, LocalTime.MAX);
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("movedAt"), end);
    }
}
