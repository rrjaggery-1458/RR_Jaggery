package com.rrjaggery.notification.controller;

import com.rrjaggery.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/notifications")
public class HealthCheckController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealth() {
        Map<String, Object> healthInfo = Map.of(
                "service", "notification-service",
                "status", "UP",
                "port", 8088,
                "schema", "notification_schema",
                "version", "1.0.0-SNAPSHOT"
        );
        return ResponseEntity.ok(ApiResponse.ok(healthInfo));
    }
}
