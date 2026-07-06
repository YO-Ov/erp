package com.hwlee.erp.master.vendoritem;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.common.entity.MasterStatus;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.vendor.Vendor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 거래처 취급품목 (구매정보레코드, SAP Purchasing Info Record 에 해당).
 *
 * <p>"이 거래처(Vendor)가 이 품목(Item)을 공급한다"는 마스터 관계 + 조건(매입단가·리드타임)을 담는다.
 * 입고(GoodsReceipt)는 이 매핑에 등록된 조합만 허용된다 — 아무 거래처에서 아무 품목이나 못 받는다.
 * (거래처×품목) 조합은 유일하다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "vendor_item",
        uniqueConstraints = @UniqueConstraint(name = "uk_vendor_item",
                columnNames = {"vendor_id", "item_id"}))
public class VendorItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** 이 거래처가 이 품목을 공급하는 단가(매입가). */
    @Column(name = "supply_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal supplyPrice;

    /** 발주 후 입고까지 걸리는 리드타임(일). */
    @Column(name = "lead_time_days", nullable = false)
    private int leadTimeDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private MasterStatus status = MasterStatus.ACTIVE;

    public static VendorItem create(Vendor vendor, Item item, BigDecimal supplyPrice, int leadTimeDays) {
        validate(vendor, item, supplyPrice, leadTimeDays);
        VendorItem vi = new VendorItem();
        vi.vendor = vendor;
        vi.item = item;
        vi.supplyPrice = supplyPrice;
        vi.leadTimeDays = leadTimeDays;
        return vi;
    }

    public void update(BigDecimal supplyPrice, int leadTimeDays) {
        if (supplyPrice == null || supplyPrice.signum() < 0)
            throw new IllegalArgumentException("supplyPrice 는 0 이상이어야 한다.");
        if (leadTimeDays < 0)
            throw new IllegalArgumentException("leadTimeDays 는 0 이상이어야 한다.");
        this.supplyPrice = supplyPrice;
        this.leadTimeDays = leadTimeDays;
    }

    public void changeStatus(MasterStatus newStatus) {
        if (newStatus == null) throw new IllegalArgumentException("status 는 null 일 수 없다.");
        this.status = newStatus;
    }

    private static void validate(Vendor vendor, Item item, BigDecimal supplyPrice, int leadTimeDays) {
        if (vendor == null) throw new IllegalArgumentException("vendor 는 null 일 수 없다.");
        if (item == null) throw new IllegalArgumentException("item 은 null 일 수 없다.");
        if (supplyPrice == null || supplyPrice.signum() < 0)
            throw new IllegalArgumentException("supplyPrice 는 0 이상이어야 한다.");
        if (leadTimeDays < 0)
            throw new IllegalArgumentException("leadTimeDays 는 0 이상이어야 한다.");
    }
}
