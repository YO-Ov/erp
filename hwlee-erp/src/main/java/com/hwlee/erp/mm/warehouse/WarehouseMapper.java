package com.hwlee.erp.mm.warehouse;

import com.hwlee.erp.mm.warehouse.dto.WarehouseResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {

    @Mapping(target = "factoryCode", source = "factory.code")
    @Mapping(target = "factoryName", source = "factory.name")
    WarehouseResponse toResponse(Warehouse entity);
}
