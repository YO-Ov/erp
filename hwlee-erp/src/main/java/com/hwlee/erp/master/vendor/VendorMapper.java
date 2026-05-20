package com.hwlee.erp.master.vendor;

import com.hwlee.erp.master.vendor.dto.VendorResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VendorMapper {

    VendorResponse toResponse(Vendor entity);
}
