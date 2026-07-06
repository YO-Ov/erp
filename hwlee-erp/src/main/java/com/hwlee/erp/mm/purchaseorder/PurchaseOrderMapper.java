package com.hwlee.erp.mm.purchaseorder;

import com.hwlee.erp.mm.purchaseorder.dto.PurchaseOrderLineResponse;
import com.hwlee.erp.mm.purchaseorder.dto.PurchaseOrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PurchaseOrderMapper {

    @Mapping(source = "vendor.id",      target = "vendorId")
    @Mapping(source = "vendor.code",    target = "vendorCode")
    @Mapping(source = "vendor.name",    target = "vendorName")
    @Mapping(source = "warehouse.id",   target = "warehouseId")
    @Mapping(source = "warehouse.code", target = "warehouseCode")
    @Mapping(source = "warehouse.name", target = "warehouseName")
    @Mapping(expression = "java(entity.totalAmount())", target = "totalAmount")
    PurchaseOrderResponse toResponse(PurchaseOrder entity);

    @Mapping(source = "item.id",   target = "itemId")
    @Mapping(source = "item.code", target = "itemCode")
    @Mapping(source = "item.name", target = "itemName")
    PurchaseOrderLineResponse toResponse(PurchaseOrderLine line);
}
