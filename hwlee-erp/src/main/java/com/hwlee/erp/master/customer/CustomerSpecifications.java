package com.hwlee.erp.master.customer;

import com.hwlee.erp.common.entity.MasterStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * Customer 목록 조회용 동적 조건.
 * Phase 1 은 Specification 으로 처리하고, Phase 2 부터 QueryDSL 로 전환한다.
 */
public final class CustomerSpecifications {

    private CustomerSpecifications() {}

    public static Specification<Customer> nameContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.like(root.get("name"), "%" + keyword + "%");
    }

    public static Specification<Customer> businessNoEquals(String businessNo) {
        if (businessNo == null || businessNo.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("businessNo"), businessNo);
    }

    public static Specification<Customer> statusEquals(MasterStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
