package com.hwlee.erp.master.item.dto;

public record ItemCategoryResponse(
        String code,
        String name,
        int sortOrder
) {}
