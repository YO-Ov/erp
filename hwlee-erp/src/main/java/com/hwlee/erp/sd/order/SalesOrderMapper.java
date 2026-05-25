package com.hwlee.erp.sd.order;

import com.hwlee.erp.sd.order.dto.SalesOrderLineResponse;
import com.hwlee.erp.sd.order.dto.SalesOrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SalesOrderMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.code", target = "customerCode")
    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "salesperson.id", target = "salespersonId")
    @Mapping(source = "salesperson.name", target = "salespersonName")
    @Mapping(source = "quotation.id", target = "quotationId")
    @Mapping(source = "quotation.number", target = "quotationNumber")
    SalesOrderResponse toResponse(SalesOrder entity);

    @Mapping(source = "item.id", target = "itemId")
    @Mapping(source = "item.code", target = "itemCode")
    @Mapping(source = "item.name", target = "itemName")
    SalesOrderLineResponse toResponse(SalesOrderLine line);
}
