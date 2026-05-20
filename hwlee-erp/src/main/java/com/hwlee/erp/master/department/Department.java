package com.hwlee.erp.master.department;

import com.hwlee.erp.common.entity.BaseEntityWithCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 부서 마스터 — 자기참조 트리 구조 (회사 → 부서 → 팀).
 *
 * <p>자동 코드 생성 대상이 아니다. 부서 코드는 의미 있는 명시적 값(예: {@code DEPT-SALES})
 * 으로 운영자가 직접 부여한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "department")
@SQLDelete(sql = "UPDATE department SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Department extends BaseEntityWithCode {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Department parent;

    public static Department create(String code, String name, Department parent) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code 는 비어 있을 수 없다.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 은 비어 있을 수 없다.");
        }
        Department d = new Department();
        d.assignCode(code);
        d.name = name;
        d.parent = parent;
        return d;
    }

    public void update(String name, Department parent) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 은 비어 있을 수 없다.");
        }
        if (parent != null && parent.getId() != null && parent.getId().equals(this.getId())) {
            throw new IllegalArgumentException("자기 자신을 부모 부서로 지정할 수 없다.");
        }
        this.name = name;
        this.parent = parent;
    }
}
