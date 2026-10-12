package com.volunteerhub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * HealthController provides system diagnostic and liveness checks for the platform.
 * It verifies application responsiveness and live MySQL database connectivity.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Volunteer Hub and Performance Tracking System");
        response.put("version", "1.0.0");
        response.put("timestamp", Instant.now().toString());

        // Check active MySQL connection via connection pool
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(2);
            response.put("database", isValid ? "CONNECTED" : "UNREACHABLE");
            response.put("databaseCatalog", connection.getCatalog());
        } catch (Exception ex) {
            response.put("database", "DISCONNECTED: " + ex.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}
