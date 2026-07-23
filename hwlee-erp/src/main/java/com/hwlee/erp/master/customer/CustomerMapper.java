package com.hwlee.erp.master.customer;

import com.hwlee.erp.master.customer.dto.CustomerContactResponse;
import com.hwlee.erp.master.customer.dto.CustomerResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toResponse(Customer entity);

    CustomerContactResponse toContactResponse(CustomerContact entity);

    List<CustomerContactResponse> toContactResponses(List<CustomerContact> entities);
}
