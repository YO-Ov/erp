package com.hwlee.erp.master.item;

import com.hwlee.erp.master.item.dto.ItemResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    ItemResponse toResponse(Item entity);
}
