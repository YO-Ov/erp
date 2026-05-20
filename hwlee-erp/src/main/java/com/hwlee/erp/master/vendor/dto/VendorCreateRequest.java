package com.hwlee.erp.master.vendor.dto;

import com.hwlee.erp.master.customer.PaymentTerms;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VendorCreateRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 20) String businessNo,
        @Size(max = 500) String address,
        @NotNull PaymentTerms paymentTerms
) {}
