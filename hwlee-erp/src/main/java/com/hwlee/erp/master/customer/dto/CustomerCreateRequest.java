package com.hwlee.erp.master.customer.dto;

import com.hwlee.erp.master.customer.PaymentTerms;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CustomerCreateRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 20) String businessNo,
        @Size(max = 500) String address,
        @NotNull @PositiveOrZero BigDecimal creditLimit,
        @NotNull PaymentTerms paymentTerms
) {}
