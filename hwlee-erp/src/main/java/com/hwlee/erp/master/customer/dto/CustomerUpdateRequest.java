package com.hwlee.erp.master.customer.dto;

import com.hwlee.erp.master.customer.PaymentTerms;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 수정 요청. business_no 는 외부 식별자라 변경 불가 정책이므로 필드에 포함하지 않는다.
 */
public record CustomerUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) String address,
        @NotNull @PositiveOrZero BigDecimal creditLimit,
        @NotNull PaymentTerms paymentTerms
) {}
