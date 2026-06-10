package com.hwlee.erp.fi.credit;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.customer.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 여신(신용한도) 상향 요청 — 영업이 "이 고객 한도를 올려달라" 고 올리고, 재무가 승인/거부한다.
 *
 * <p>실무의 신용 통제: 한도 초과는 영업이 데이터로 우회하는 게 아니라, 재무가 그 고객의 신용도를
 * 재평가해 한도를 조정하는 의사결정 문제다. 요청자({@code createdBy})는 영업, 결정자
 * ({@link #decidedBy})는 재무로 분리된다. 승인 시 호출 측 서비스가 고객 한도를 실제로 올린다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "credit_limit_request")
public class CreditLimitRequest extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30, updatable = false)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** 요청 시점의 현재 한도(스냅샷, 근거). */
    @Column(name = "current_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentLimit;

    /** 올려달라는 목표 한도. */
    @Column(name = "requested_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedLimit;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CreditLimitRequestStatus status = CreditLimitRequestStatus.PENDING;

    /** 결정한 재무 담당자(username). 결정 전 null. */
    @Column(name = "decided_by", length = 64)
    private String decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    public static CreditLimitRequest submit(String number, Customer customer,
                                            BigDecimal currentLimit, BigDecimal requestedLimit, String reason) {
        if (number == null || number.isBlank()) throw new IllegalArgumentException("number 는 비어 있을 수 없다.");
        if (customer == null) throw new IllegalArgumentException("customer 는 null 일 수 없다.");
        if (requestedLimit == null || requestedLimit.signum() <= 0)
            throw new IllegalArgumentException("요청 한도는 0보다 커야 한다.");
        if (currentLimit != null && requestedLimit.compareTo(currentLimit) <= 0)
            throw new IllegalArgumentException(
                    "요청 한도는 현재 한도보다 커야 합니다 (현재 " + currentLimit + ", 요청 " + requestedLimit + ").");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("사유는 필수입니다.");

        CreditLimitRequest r = new CreditLimitRequest();
        r.number = number;
        r.customer = customer;
        r.currentLimit = currentLimit;
        r.requestedLimit = requestedLimit;
        r.reason = reason;
        return r;
    }

    /** 승인 — PENDING → APPROVED. 고객 한도 실제 상향은 호출 측 서비스가 수행한다. */
    public void approve(String decidedBy, String note, LocalDateTime now) {
        ensurePending("승인");
        this.status = CreditLimitRequestStatus.APPROVED;
        stampDecision(decidedBy, note, now);
    }

    /** 거부 — PENDING → REJECTED. */
    public void reject(String decidedBy, String note, LocalDateTime now) {
        ensurePending("거부");
        this.status = CreditLimitRequestStatus.REJECTED;
        stampDecision(decidedBy, note, now);
    }

    private void ensurePending(String action) {
        if (status != CreditLimitRequestStatus.PENDING)
            throw new IllegalStateException("대기(PENDING) 요청만 " + action + "할 수 있습니다. 현재: " + status);
    }

    private void stampDecision(String decidedBy, String note, LocalDateTime now) {
        this.decidedBy = decidedBy;
        this.decisionNote = note;
        this.decidedAt = now;
    }
}
