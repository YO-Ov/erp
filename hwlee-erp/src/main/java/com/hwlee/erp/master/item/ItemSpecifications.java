package com.hwlee.erp.master.item;

import com.hwlee.erp.common.entity.MasterStatus;
import org.springframework.data.jpa.domain.Specification;

public final class ItemSpecifications {

    private ItemSpecifications() {}

    public static Specification<Item> nameContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.like(root.get("name"), "%" + keyword + "%");
    }

    public static Specification<Item> categoryEquals(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Item> statusEquals(MasterStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
