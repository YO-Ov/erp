package com.hwlee.erp.mm.warehouse;

import com.hwlee.erp.mm.warehouse.dto.WarehouseResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {

    WarehouseResponse toResponse(Warehouse entity);
}
