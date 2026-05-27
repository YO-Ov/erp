package com.hwlee.erp.mm.goodsissue;

import com.hwlee.erp.mm.goodsissue.dto.GoodsIssueLineResponse;
import com.hwlee.erp.mm.goodsissue.dto.GoodsIssueResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GoodsIssueMapper {

    @Mapping(source = "warehouse.id",   target = "warehouseId")
    @Mapping(source = "warehouse.code", target = "warehouseCode")
    @Mapping(source = "warehouse.name", target = "warehouseName")
    GoodsIssueResponse toResponse(GoodsIssue entity);

    @Mapping(source = "item.id",   target = "itemId")
    @Mapping(source = "item.code", target = "itemCode")
    @Mapping(source = "item.name", target = "itemName")
    GoodsIssueLineResponse toResponse(GoodsIssueLine line);
}
