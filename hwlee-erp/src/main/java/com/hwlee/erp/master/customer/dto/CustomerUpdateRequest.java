package com.hwlee.erp.master.customer.dto;

import com.hwlee.erp.master.customer.PaymentTerms;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 고객 수정 요청 (영업). business_no(외부 식별자)와 creditLimit(여신 = 재무 권한)은 포함하지 않는다.
 * 한도 변경은 여신 요청/승인으로만 이뤄진다(권한 분리).
 */
public record CustomerUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) String address,
        @NotNull PaymentTerms paymentTerms
) {}
