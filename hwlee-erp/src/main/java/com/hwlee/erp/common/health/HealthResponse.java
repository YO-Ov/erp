package com.hwlee.erp.common.health;

import java.time.OffsetDateTime;

public record HealthResponse(
        String status,
        String db,
        OffsetDateTime timestamp
) {
    public static HealthResponse up(String dbStatus) {
        return new HealthResponse("UP", dbStatus, OffsetDateTime.now());
    }

    public static HealthResponse down(String dbStatus) {
        return new HealthResponse("DOWN", dbStatus, OffsetDateTime.now());
    }
}
