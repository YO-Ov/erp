package com.hwlee.erp.master.vendor;

import com.hwlee.erp.common.entity.BaseEntityWithCode;
import com.hwlee.erp.master.customer.PaymentTerms;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * 거래처 마스터 — 매입(MM) 에서 입고할 대상.
 * Customer 와 거의 동일하지만 credit_limit 이 없다 (우리가 사는 입장이라 신용 평가가 반대 방향).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "vendor")
@SQLDelete(sql = "UPDATE vendor SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Vendor extends BaseEntityWithCode {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "business_no", nullable = false, unique = true, length = 20)
    private String businessNo;

    @Column(name = "address", length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_terms", nullable = false, length = 30)
    private PaymentTerms paymentTerms;

    public static Vendor create(String code, String name, String businessNo,
                                String address, PaymentTerms paymentTerms) {
        validate(name, businessNo, paymentTerms);
        Vendor v = new Vendor();
        v.assignCode(code);
        v.name = name;
        v.businessNo = businessNo;
        v.address = address;
        v.paymentTerms = paymentTerms;
        return v;
    }

    public void update(String name, String address, PaymentTerms paymentTerms) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 은 비어 있을 수 없다.");
        }
        if (paymentTerms == null) {
            throw new IllegalArgumentException("paymentTerms 은 null 일 수 없다.");
        }
        this.name = name;
        this.address = address;
        this.paymentTerms = paymentTerms;
    }

    private static void validate(String name, String businessNo, PaymentTerms paymentTerms) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 은 비어 있을 수 없다.");
        }
        if (businessNo == null || businessNo.isBlank()) {
            throw new IllegalArgumentException("businessNo 는 비어 있을 수 없다.");
        }
        if (paymentTerms == null) {
            throw new IllegalArgumentException("paymentTerms 은 null 일 수 없다.");
        }
    }
}
