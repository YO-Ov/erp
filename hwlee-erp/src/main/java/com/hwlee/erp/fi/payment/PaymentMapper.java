package com.hwlee.erp.fi.payment;

import com.hwlee.erp.fi.payment.dto.PaymentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.code", target = "customerCode")
    @Mapping(source = "vendor.id", target = "vendorId")
    @Mapping(source = "vendor.code", target = "vendorCode")
    PaymentResponse toResponse(Payment entity);
}
