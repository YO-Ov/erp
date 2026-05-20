package com.hwlee.erp.master.customer;

import com.hwlee.erp.master.customer.dto.CustomerResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toResponse(Customer entity);
}
