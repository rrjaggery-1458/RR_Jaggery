package com.rrjaggery.auth.controller;

import com.rrjaggery.auth.dto.AuthResponse;
import com.rrjaggery.auth.dto.LoginRequest;
import com.rrjaggery.auth.dto.RegisterRequest;
import com.rrjaggery.auth.dto.UserDto;
import com.rrjaggery.auth.service.AuthService;
import com.rrjaggery.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response, "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Login successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getMe(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated", "AUTH_REQUIRED"));
        }
        UserDto userDto = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(userDto));
    }
}
