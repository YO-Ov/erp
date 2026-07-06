package com.hwlee.erp.master.vendoritem;

import com.hwlee.erp.common.entity.MasterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VendorItemRepository
        extends JpaRepository<VendorItem, Long>, JpaSpecificationExecutor<VendorItem> {

    boolean existsByVendorIdAndItemId(Long vendorId, Long itemId);

    /** 입고 검증용 — 이 거래처가 이 품목을 "현재 취급(ACTIVE)"하는지. */
    boolean existsByVendorIdAndItemIdAndStatus(Long vendorId, Long itemId, MasterStatus status);
}
