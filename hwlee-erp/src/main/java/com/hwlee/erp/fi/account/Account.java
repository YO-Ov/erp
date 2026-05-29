package com.hwlee.erp.fi.account;

import com.hwlee.erp.common.entity.BaseEntityWithCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * 계정과목 마스터 — 회계의 첫 번째 마스터.
 *
 * <p>{@link com.hwlee.erp.master.department.Department} 와 동일한 자기참조 트리 구조.
 * 자산 1000(헤더) → 매출채권 1200(말단). 자동 코드 생성은 쓰지 않는다 — 회계 코드는 의미 있는 명시적 값.
 *
 * <p>핵심 결정:
 * <ul>
 *   <li>{@link #type} 이 정상 잔액 방향의 단일 진실 — 별도 normal_side 컬럼 없음.</li>
 *   <li>{@link #postable} = false 인 헤더 계정은 전표 라인의 account 로 쓸 수 없다
 *       ({@link com.hwlee.erp.fi.journal.JournalEntry#addLine} 가 검증).</li>
 * </ul>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "account")
@SQLDelete(sql = "UPDATE account SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Account extends BaseEntityWithCode {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private AccountType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Account parent;

    @Column(name = "postable", nullable = false)
    private boolean postable;

    public static Account create(String code, String name, AccountType type, Account parent, boolean postable) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code 는 비어 있을 수 없다.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 은 비어 있을 수 없다.");
        }
        if (type == null) {
            throw new IllegalArgumentException("type 은 null 일 수 없다.");
        }
        Account a = new Account();
        a.assignCode(code);
        a.name = name;
        a.type = type;
        a.parent = parent;
        a.postable = postable;
        return a;
    }

    public void update(String name, Account parent, boolean postable) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 은 비어 있을 수 없다.");
        }
        if (parent != null && parent.getId() != null && parent.getId().equals(this.getId())) {
            throw new IllegalArgumentException("자기 자신을 부모 계정으로 지정할 수 없다.");
        }
        this.name = name;
        this.parent = parent;
        this.postable = postable;
    }

    /** 정상 잔액 방향 — {@link AccountType} 에서 파생. */
    public NormalSide normalSide() {
        return type.getNormalSide();
    }
}
