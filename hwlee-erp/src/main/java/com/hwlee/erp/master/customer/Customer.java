package com.hwlee.erp.master.customer;

import com.hwlee.erp.audit.AuditEntityListener;
import com.hwlee.erp.audit.Auditable;
import com.hwlee.erp.common.entity.BaseEntityWithCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
     * 담당자(연락처) — 애그리거트 자식. 대표 담당자를 먼저 보이도록 정렬한다.
     * 추가/수정/삭제는 아래 도메인 메서드로만(외부에서 컬렉션 직접 변경 불가).
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("primary DESC, id ASC")
    private List<CustomerContact> contacts = new ArrayList<>();

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

    /**
     * 기본정보 수정 (영업 권한). 신용한도(creditLimit)·사업자번호(business_no)는 건드리지 않는다 —
     * 한도는 여신(재무) 권한, 사업자번호는 외부 식별자라 불변.
     */
    public void updateBasicInfo(String name, String address, PaymentTerms paymentTerms) {
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

    /**
     * 신용한도만 변경 — 여신 상향 요청 승인(FI) 시 호출. 다른 필드는 건드리지 않는다.
     * "누가 언제 한도를 바꿨나" 는 {@link #auditSnapshot()} 으로 감사 로그에 남는다.
     */
    public void changeCreditLimit(BigDecimal newCreditLimit) {
        if (newCreditLimit == null || newCreditLimit.signum() < 0) {
            throw new IllegalArgumentException("creditLimit 은 0 이상이어야 한다.");
        }
        this.creditLimit = newCreditLimit;
    }

    // ── 담당자(연락처) ─────────────────────────────────────
    // 대표 담당자(primary)는 고객당 최대 1명. 이 불변식을 여기서 강제한다.

    /** 담당자 추가. primary=true 면 기존 대표를 해제하고 이 담당자를 대표로 세운다. */
    public CustomerContact addContact(String name, String position, String phone, String email, boolean primary) {
        if (primary) {
            contacts.forEach(c -> c.assignPrimary(false));
        }
        CustomerContact contact = new CustomerContact(this, name, position, phone, email, primary);
        contacts.add(contact);
        return contact;
    }

    /** 담당자 수정. primary 지정 시 다른 담당자의 대표 표시를 해제한다. */
    public CustomerContact updateContact(Long contactId, String name, String position, String phone,
                                         String email, boolean primary) {
        CustomerContact contact = findContact(contactId);
        contact.update(name, position, phone, email);
        if (primary) {
            contacts.forEach(c -> c.assignPrimary(false));
            contact.assignPrimary(true);
        } else {
            contact.assignPrimary(false);
        }
        return contact;
    }

    /** 담당자 삭제 — orphanRemoval 로 실제 행이 제거된다(연락처는 이력 보존 대상 아님). */
    public void removeContact(Long contactId) {
        contacts.remove(findContact(contactId));
    }

    public List<CustomerContact> getContacts() {
        return Collections.unmodifiableList(contacts);
    }

    private CustomerContact findContact(Long contactId) {
        return contacts.stream()
                .filter(c -> contactId != null && contactId.equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("담당자를 찾을 수 없습니다: id=" + contactId));
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
