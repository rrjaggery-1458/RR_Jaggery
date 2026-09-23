package com.rrjaggery.auth.controller;

import com.rrjaggery.auth.dto.AdminStatsDto;
import com.rrjaggery.auth.dto.CreateManagerRequest;
import com.rrjaggery.auth.dto.UpdateUserAdminRequest;
import com.rrjaggery.auth.dto.UserDto;
import com.rrjaggery.auth.service.AdminUserService;
import com.rrjaggery.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/auth/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PostMapping("/managers")
    public ResponseEntity<ApiResponse<UserDto>> createManager(@Valid @RequestBody CreateManagerRequest request) {
        UserDto manager = adminUserService.createManager(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(manager, "Manager account created successfully."));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok(adminUserService.getAllUsers()));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable UUID id,
            @RequestBody UpdateUserAdminRequest request) {
        UserDto updated = adminUserService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "User updated successfully."));
    }

    @PatchMapping("/users/{id}/enabled")
    public ResponseEntity<ApiResponse<UserDto>> toggleUserEnabled(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", true);
        UserDto updated = adminUserService.toggleUserEnabled(id, enabled);
        return ResponseEntity.ok(ApiResponse.ok(updated, "User status updated successfully."));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsDto>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(adminUserService.getStats()));
    }
}
