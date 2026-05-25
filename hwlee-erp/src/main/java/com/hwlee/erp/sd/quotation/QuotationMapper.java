package com.hwlee.erp.sd.quotation;

import com.hwlee.erp.sd.quotation.dto.QuotationLineResponse;
import com.hwlee.erp.sd.quotation.dto.QuotationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface QuotationMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.code", target = "customerCode")
    @Mapping(source = "customer.name", target = "customerName")
    QuotationResponse toResponse(Quotation entity);

    @Mapping(source = "item.id", target = "itemId")
    @Mapping(source = "item.code", target = "itemCode")
    @Mapping(source = "item.name", target = "itemName")
    QuotationLineResponse toResponse(QuotationLine line);
}
