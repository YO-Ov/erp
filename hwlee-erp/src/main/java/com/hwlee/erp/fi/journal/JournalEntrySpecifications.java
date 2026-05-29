package com.hwlee.erp.fi.journal;

import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/**
 * 전표 검색 조건 — Phase 2 의 {@code InvoiceSpecifications} 와 같은 패턴.
 * QueryDSL 도입 전이라 Spring Data JPA Specification 으로 동적 조건을 합친다.
 */
public final class JournalEntrySpecifications {

    private JournalEntrySpecifications() {}

    public static Specification<JournalEntry> sourceTypeEquals(JournalSource sourceType) {
        return (root, query, cb) -> sourceType == null ? null : cb.equal(root.get("sourceType"), sourceType);
    }

    public static Specification<JournalEntry> sourceIdEquals(Long sourceId) {
        return (root, query, cb) -> sourceId == null ? null : cb.equal(root.get("sourceId"), sourceId);
    }

    public static Specification<JournalEntry> statusEquals(JournalEntryStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<JournalEntry> entryDateFrom(LocalDate from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("entryDate"), from);
    }

    public static Specification<JournalEntry> entryDateTo(LocalDate to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("entryDate"), to);
    }
}
