package com.hwlee.erp.fi.payment.dto;

import com.hwlee.erp.fi.payment.PaymentStatus;
import com.hwlee.erp.fi.payment.PaymentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String number,
        PaymentType type,
        Long customerId,
        String customerCode,
        Long vendorId,
        String vendorCode,
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentStatus status,
        LocalDateTime postedAt,
        String description,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
