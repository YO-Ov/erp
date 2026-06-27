package com.hwlee.erp.master.item;

import com.hwlee.erp.common.entity.MasterStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long> {

    /** 신규/수정 품목에 부여 가능한 카테고리인지(존재 + 활성) 검증용. */
    boolean existsByCodeAndStatus(String code, MasterStatus status);

    /** 드롭다운/목록용 — 활성 카테고리를 정렬 순서대로. */
    List<ItemCategory> findAllByStatusOrderBySortOrderAscCodeAsc(MasterStatus status);
}
