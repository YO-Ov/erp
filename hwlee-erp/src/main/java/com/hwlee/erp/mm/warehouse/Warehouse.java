package com.hwlee.erp.mm.warehouse;

import com.hwlee.erp.common.entity.BaseEntityWithCode;
import com.hwlee.erp.master.factory.Factory;
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
 * 창고 마스터 — 재고가 실제로 있는 장소.
 *
 * <p>자동 코드 생성 대상이 아니다. 창고 코드는 의미 있는 명시적 값({@code WH-HQ}, {@code WH-BUS})
 * 으로 운영자가 직접 부여한다 (Department 와 같은 방침).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "warehouse")
@SQLDelete(sql = "UPDATE warehouse SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Warehouse extends BaseEntityWithCode {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    /** 소속 공장. 본사/물류 창고처럼 특정 공장에 속하지 않을 수 있어 nullable. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_id")
    private Factory factory;

    public static Warehouse create(String code, String name, String address) {
        validate(code, name);
        Warehouse w = new Warehouse();
        w.assignCode(code);
        w.name = name;
        w.address = address;
        return w;
    }

    /** 소속 공장 지정/변경(본사·물류 창고는 null 가능). */
    public void assignFactory(Factory factory) {
        this.factory = factory;
    }

    public void update(String name, String address) {
        validate(this.getCode(), name);
        this.name = name;
        this.address = address;
    }

    private static void validate(String code, String name) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code 는 비어 있을 수 없다.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 은 비어 있을 수 없다.");
        }
    }
}
