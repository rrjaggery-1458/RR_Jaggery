package com.rrjaggery.auth.controller;

import com.rrjaggery.auth.dto.SystemHealthDto;
import com.rrjaggery.auth.service.SystemHealthService;
import com.rrjaggery.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/auth/admin")
@PreAuthorize("hasRole('ADMIN')")
public class SystemHealthController {

    private final SystemHealthService systemHealthService;

    public SystemHealthController(SystemHealthService systemHealthService) {
        this.systemHealthService = systemHealthService;
    }

    @GetMapping("/system-health")
    public ResponseEntity<ApiResponse<SystemHealthDto>> getSystemHealth() {
        SystemHealthDto healthSnapshot = systemHealthService.getSystemHealthSnapshot();
        return ResponseEntity.ok(ApiResponse.ok(healthSnapshot));
    }
}
