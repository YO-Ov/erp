package com.hwlee.erp.common.health;

import javax.sql.DataSource;
import java.sql.Connection;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        try (Connection conn = dataSource.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT 1")) {
            rs.next();
            return ResponseEntity.ok(HealthResponse.up("OK"));
        } catch (Exception e) {
            return ResponseEntity.status(503)
                    .body(HealthResponse.down("FAIL: " + e.getMessage()));
        }
    }
}
