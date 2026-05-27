package com.hwlee.erp.mm.warehouse;

import com.hwlee.erp.common.entity.MasterStatus;
import org.springframework.data.jpa.domain.Specification;

public final class WarehouseSpecifications {

    private WarehouseSpecifications() {}

    public static Specification<Warehouse> nameContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return (root, q, cb) -> cb.like(root.get("name"), "%" + keyword + "%");
    }

    public static Specification<Warehouse> statusEquals(MasterStatus status) {
        if (status == null) return null;
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }
}
