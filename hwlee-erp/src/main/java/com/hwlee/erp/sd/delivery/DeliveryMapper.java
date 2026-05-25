package com.hwlee.erp.sd.delivery;

import com.hwlee.erp.sd.delivery.dto.DeliveryLineResponse;
import com.hwlee.erp.sd.delivery.dto.DeliveryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeliveryMapper {

    @Mapping(source = "salesOrder.id", target = "salesOrderId")
    @Mapping(source = "salesOrder.number", target = "salesOrderNumber")
    DeliveryResponse toResponse(Delivery entity);

    @Mapping(source = "salesOrderLine.id", target = "salesOrderLineId")
    @Mapping(source = "salesOrderLine.item.id", target = "itemId")
    @Mapping(source = "salesOrderLine.item.code", target = "itemCode")
    @Mapping(source = "salesOrderLine.item.name", target = "itemName")
    DeliveryLineResponse toResponse(DeliveryLine line);
}
