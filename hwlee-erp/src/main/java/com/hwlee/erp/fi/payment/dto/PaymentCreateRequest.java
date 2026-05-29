package com.hwlee.erp.fi.payment.dto;

import com.hwlee.erp.fi.payment.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 입금/출금 등록 요청. type/party 정합성:
 * <ul>
 *   <li>RECEIPT 면 customerId 필수, vendorId null.</li>
 *   <li>DISBURSEMENT 면 vendorId 필수, customerId null.</li>
 * </ul>
 * 서비스가 validation. (Bean Validation 으로 조건부 검증은 복잡해 빼고 도메인 팩토리가 강제.)
 */
public record PaymentCreateRequest(
        @NotNull PaymentType type,
        Long customerId,
        Long vendorId,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate paymentDate,
        String description
) {}
