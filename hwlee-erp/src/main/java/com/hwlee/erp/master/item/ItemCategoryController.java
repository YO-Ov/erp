package com.hwlee.erp.master.item;

import com.hwlee.erp.common.entity.MasterStatus;
import com.hwlee.erp.master.item.dto.ItemCategoryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 품목 카테고리 마스터 조회 API — 품목 등록/검색 화면의 카테고리 드롭다운을 데이터로 채운다.
 * (생성/수정은 카테고리를 자주 바꾸지 않으므로 우선 시드/마이그레이션으로만 관리한다.)
 */
@RestController
@RequestMapping("/api/item-categories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES','PURCHASING','FINANCE','PRODUCTION','ADMIN')")
public class ItemCategoryController {

    private final ItemCategoryRepository repository;

    @GetMapping
    public List<ItemCategoryResponse> list() {
        return repository.findAllByStatusOrderBySortOrderAscCodeAsc(MasterStatus.ACTIVE).stream()
                .map(c -> new ItemCategoryResponse(c.getCode(), c.getName(), c.getSortOrder()))
                .toList();
    }
}
