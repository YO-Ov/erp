package com.hwlee.erp.sd.delivery.dto;

import com.hwlee.erp.sd.delivery.DeliveryStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DeliveryResponse(
        Long id,
        String number,
        Long salesOrderId,
        String salesOrderNumber,
        DeliveryStatus status,
        LocalDate shippedDate,
        List<DeliveryLineResponse> lines,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}
