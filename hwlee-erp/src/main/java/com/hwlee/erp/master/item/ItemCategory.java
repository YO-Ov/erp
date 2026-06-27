package com.hwlee.erp.master.item;

import com.hwlee.erp.common.entity.BaseEntityWithCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * 품목 카테고리 마스터 — 기존 자바 enum 을 대체한다.
 *
 * <p>노트북/모니터로 시작했으나, 데스크탑·키보드·마우스 등 라인업을 <b>코드 수정 없이 데이터로</b>
 * 늘리기 위해 마스터 테이블로 일반화했다. {@code Item.category} 컬럼이 이 엔티티의 {@code code} 를
 * 참조한다(FK).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "item_category")
@SQLDelete(sql = "UPDATE item_category SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ItemCategory extends BaseEntityWithCode {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public static ItemCategory create(String code, String name, int sortOrder) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code 는 비어 있을 수 없다.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 은 비어 있을 수 없다.");
        }
        ItemCategory category = new ItemCategory();
        category.assignCode(code);
        category.name = name;
        category.sortOrder = sortOrder;
        return category;
    }

    public void update(String name, int sortOrder) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 은 비어 있을 수 없다.");
        }
        this.name = name;
        this.sortOrder = sortOrder;
    }
}
