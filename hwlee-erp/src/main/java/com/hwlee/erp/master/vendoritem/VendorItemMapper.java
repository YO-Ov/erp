package com.hwlee.erp.master.vendoritem;

import com.hwlee.erp.master.vendoritem.dto.VendorItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VendorItemMapper {

    @Mapping(source = "vendor.id",   target = "vendorId")
    @Mapping(source = "vendor.code", target = "vendorCode")
    @Mapping(source = "vendor.name", target = "vendorName")
    @Mapping(source = "item.id",     target = "itemId")
    @Mapping(source = "item.code",   target = "itemCode")
    @Mapping(source = "item.name",   target = "itemName")
    VendorItemResponse toResponse(VendorItem entity);
}
