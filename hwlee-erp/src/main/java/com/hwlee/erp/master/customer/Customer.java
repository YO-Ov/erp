package com.hwlee.erp.master.customer;

import com.hwlee.erp.audit.AuditEntityListener;
import com.hwlee.erp.audit.Auditable;
import com.hwlee.erp.common.entity.BaseEntityWithCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * 고객 마스터 — 영업(SD)에서 수주를 발행할 대상.
 *
 * <p>Soft Delete: {@code DELETE} 요청은 {@code @SQLDelete} 에 의해 UPDATE 로 변환되어
 * {@code deleted_at} 컬럼에 시각을 기록한다. 일반 조회는 {@code @SQLRestriction} 으로
 * 자동으로 {@code deleted_at IS NULL} 이 붙어 삭제된 행이 보이지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "customer")
@SQLDelete(sql = "UPDATE customer SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
// Phase 6: 감사 대상(선택 B). BaseEntity 의 AuditingEntityListener 와 함께 동작(둘 다 실행).
@EntityListeners(AuditEntityListener.class)
public class Customer extends BaseEntityWithCode implements Auditable {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "business_no", nullable = false, unique = true, length = 20)
    private String businessNo;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "credit_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal creditLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_terms", nullable = false, length = 30)
    private PaymentTerms paymentTerms;

    /**
     * 생성자 — code 는 외부에서 발급받아 주입한다.
     */
    public static Customer create(String code, String name, String businessNo, String address,
                                  BigDecimal creditLimit, PaymentTerms paymentTerms) {
        validate(name, businessNo, creditLimit, paymentTerms);
        Customer c = new Customer();
        c.assignCode(code);
        c.name = name;
        c.businessNo = businessNo;
        c.address = address;
        c.creditLimit = creditLimit;
        c.paymentTerms = paymentTerms;
        return c;
    }

    public void update(String name, String address, BigDecimal creditLimit, PaymentTerms paymentTerms) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 은 비어 있을 수 없다.");
        }
        if (creditLimit == null || creditLimit.signum() < 0) {
            throw new IllegalArgumentException("creditLimit 은 0 이상이어야 한다.");
        }
        if (paymentTerms == null) {
            throw new IllegalArgumentException("paymentTerms 은 null 일 수 없다.");
        }
        this.name = name;
        this.address = address;
        this.creditLimit = creditLimit;
        this.paymentTerms = paymentTerms;
        // 비즈니스 규칙: business_no 는 수정 불가 (외부 식별자).
    }

    /** 감사 로그에 남길 스냅샷 — 어떤 필드를 추적할지 명시적으로 고른다. */
    @Override
    public Map<String, Object> auditSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("code", getCode());
        snapshot.put("name", name);
        snapshot.put("businessNo", businessNo);
        snapshot.put("creditLimit", creditLimit);
        snapshot.put("paymentTerms", paymentTerms);
        snapshot.put("status", getStatus());
        return snapshot;
    }

    private static void validate(String name, String businessNo, BigDecimal creditLimit, PaymentTerms paymentTerms) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 은 비어 있을 수 없다.");
        }
        if (businessNo == null || businessNo.isBlank()) {
            throw new IllegalArgumentException("businessNo 는 비어 있을 수 없다.");
        }
        if (creditLimit == null || creditLimit.signum() < 0) {
            throw new IllegalArgumentException("creditLimit 은 0 이상이어야 한다.");
        }
        if (paymentTerms == null) {
            throw new IllegalArgumentException("paymentTerms 은 null 일 수 없다.");
        }
    }
}
