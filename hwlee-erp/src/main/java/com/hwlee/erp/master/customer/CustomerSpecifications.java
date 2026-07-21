package com.hwlee.erp.master.customer;

import com.hwlee.erp.common.entity.MasterStatus;
import java.time.LocalDate;
import java.time.LocalTime;
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

    /**
     * 등록일({@code createdAt}) 하한 — "이번 달 신규 등록 고객" 같은 조회용.
     *
     * <p>고객은 거래가 아니라 마스터 데이터라 '기간' 이 곧 '등록 시점' 이다.
     * {@code createdAt} 은 시각까지 있는 {@code LocalDateTime}({@code BaseEntity} 상속)이므로
     * 날짜를 그 날 00:00:00 으로 넓혀 비교한다.
     */
    public static Specification<Customer> createdFrom(LocalDate from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay());
    }

    /** 등록일 상한 — 종료일 당일이 빠지지 않도록 그 날 23:59:59.999999999 까지 포함한다. */
    public static Specification<Customer> createdTo(LocalDate to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("createdAt"), to.atTime(LocalTime.MAX));
    }
}
