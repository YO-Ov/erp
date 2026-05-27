package com.hwlee.erp.mm.goodsreceipt;

import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptLineResponse;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GoodsReceiptMapper {

    @Mapping(source = "vendor.id",      target = "vendorId")
    @Mapping(source = "vendor.code",    target = "vendorCode")
    @Mapping(source = "vendor.name",    target = "vendorName")
    @Mapping(source = "warehouse.id",   target = "warehouseId")
    @Mapping(source = "warehouse.code", target = "warehouseCode")
    @Mapping(source = "warehouse.name", target = "warehouseName")
    GoodsReceiptResponse toResponse(GoodsReceipt entity);

    @Mapping(source = "item.id",   target = "itemId")
    @Mapping(source = "item.code", target = "itemCode")
    @Mapping(source = "item.name", target = "itemName")
    GoodsReceiptLineResponse toResponse(GoodsReceiptLine line);
}
