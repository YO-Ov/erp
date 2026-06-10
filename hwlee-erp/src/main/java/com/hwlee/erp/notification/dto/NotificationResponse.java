package com.hwlee.erp.notification.dto;

import com.hwlee.erp.notification.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        String linkUrl,
        boolean read,
        LocalDateTime createdAt
) {}
