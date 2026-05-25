package com.hwlee.erp.sd.invoice;

import com.hwlee.erp.sd.invoice.dto.InvoiceLineResponse;
import com.hwlee.erp.sd.invoice.dto.InvoiceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(source = "salesOrder.id", target = "salesOrderId")
    @Mapping(source = "salesOrder.number", target = "salesOrderNumber")
    InvoiceResponse toResponse(Invoice entity);

    @Mapping(source = "salesOrderLine.id", target = "salesOrderLineId")
    @Mapping(source = "salesOrderLine.item.id", target = "itemId")
    @Mapping(source = "salesOrderLine.item.code", target = "itemCode")
    @Mapping(source = "salesOrderLine.item.name", target = "itemName")
    InvoiceLineResponse toResponse(InvoiceLine line);
}
