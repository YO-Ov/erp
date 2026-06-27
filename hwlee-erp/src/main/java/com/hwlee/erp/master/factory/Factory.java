package com.hwlee.erp.master.factory;

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
 * 공장(생산 사업장) 마스터 — 창고·생산설비가 소속되는 물리적 거점.
 *
 * <p>창고({@code warehouse.factory_id})가 어느 공장에 속하는지를 가리키고, 생산지시는 그 창고를
 * 통해 공장에 연결된다. 자동 코드 생성 대상이 아니다 — 공장 코드는 의미 있는 명시적 값
 * ({@code FAC-01} 등)으로 운영자가 직접 부여한다(Department/Warehouse 와 같은 방침).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "factory")
@SQLDelete(sql = "UPDATE factory SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Factory extends BaseEntityWithCode {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    public static Factory create(String code, String name, String address) {
        validate(code, name);
        Factory f = new Factory();
        f.assignCode(code);
        f.name = name;
        f.address = address;
        return f;
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
