package com.hwlee.erp.master.customer;

import com.hwlee.erp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객 담당자(연락처) — 한 고객에 여러 담당자를 둔다(구매담당·경리담당·현장담당 등).
 *
 * <p>{@link Customer} 애그리거트의 자식으로, 추가/수정/삭제는 언제나 부모(Customer)의 도메인
 * 메서드를 통해서만 이뤄진다({@code addContact}/{@code updateContact}/{@code removeContact}).
 * 대표 담당자({@code primary})는 고객당 최대 1명이며, 이 불변식도 부모가 보장한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "customer_contact")
public class CustomerContact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 부서·직책 (예: "구매팀 과장"). 구조화 없이 단일 필드. */
    @Column(name = "position", length = 100)
    private String position;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 200)
    private String email;

    /** 대표 담당자 여부 — 고객당 1명. 우선 표시/기본 연락 대상. */
    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    CustomerContact(Customer customer, String name, String position, String phone, String email, boolean primary) {
        validate(name);
        this.customer = customer;
        this.name = name;
        this.position = position;
        this.phone = phone;
        this.email = email;
        this.primary = primary;
    }

    void update(String name, String position, String phone, String email) {
        validate(name);
        this.name = name;
        this.position = position;
        this.phone = phone;
        this.email = email;
    }

    /** 대표 지정/해제 — 부모(Customer)가 "대표는 1명" 불변식을 지키며 호출한다. */
    void assignPrimary(boolean primary) {
        this.primary = primary;
    }

    private static void validate(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("담당자명은 비어 있을 수 없다.");
        }
    }
}
