package com.hwlee.erp.master.customer.dto;

import com.hwlee.erp.master.customer.PaymentTerms;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 고객 등록 요청 (영업). 신용한도는 포함하지 않는다 — 신규 고객은 한도 0(현금거래)으로 시작하고,
 * 한도 부여/상향은 재무의 여신(CreditLimitRequest) 승인으로만 이뤄진다(권한 분리).
 */
public record CustomerCreateRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 20) String businessNo,
        @Size(max = 500) String address,
        @NotNull PaymentTerms paymentTerms
) {}
