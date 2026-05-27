package com.hwlee.erp.mm.stock;

import com.hwlee.erp.mm.stock.dto.StockMovementResponse;
import com.hwlee.erp.mm.stock.dto.StockResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockMapper {

    @Mapping(source = "item.id",        target = "itemId")
    @Mapping(source = "item.code",      target = "itemCode")
    @Mapping(source = "item.name",      target = "itemName")
    @Mapping(source = "warehouse.id",   target = "warehouseId")
    @Mapping(source = "warehouse.code", target = "warehouseCode")
    @Mapping(source = "warehouse.name", target = "warehouseName")
    StockResponse toResponse(Stock stock);

    @Mapping(source = "item.id",        target = "itemId")
    @Mapping(source = "item.code",      target = "itemCode")
    @Mapping(source = "item.name",      target = "itemName")
    @Mapping(source = "warehouse.id",   target = "warehouseId")
    @Mapping(source = "warehouse.code", target = "warehouseCode")
    StockMovementResponse toResponse(StockMovement movement);
}
