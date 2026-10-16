package com.rrjaggery.auth.controller;

import com.rrjaggery.auth.dto.AuthResponse;
import com.rrjaggery.auth.dto.ForgotPasswordChallengeResponse;
import com.rrjaggery.auth.dto.ForgotPasswordRequest;
import com.rrjaggery.auth.dto.LoginRequest;
import com.rrjaggery.auth.dto.RegistrationEmailStatusResponse;
import com.rrjaggery.auth.dto.RegistrationChallengeResponse;
import com.rrjaggery.auth.dto.RegisterRequest;
import com.rrjaggery.auth.dto.ResetPasswordRequest;
import com.rrjaggery.auth.dto.UserDto;
import com.rrjaggery.auth.dto.VerifyRegistrationRequest;
import com.rrjaggery.auth.dto.VerifyResetOtpRequest;
import com.rrjaggery.auth.service.AuthService;
import com.rrjaggery.auth.service.PasswordResetService;
import com.rrjaggery.auth.service.PublicRegistrationService;
import com.rrjaggery.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PublicRegistrationService registrationService;
    private final PasswordResetService passwordResetService;

    public AuthController(
            AuthService authService,
            PublicRegistrationService registrationService,
            PasswordResetService passwordResetService) {
        this.authService = authService;
        this.registrationService = registrationService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegistrationChallengeResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegistrationChallengeResponse response = new RegistrationChallengeResponse(
                registrationService.requestRegistrationCode(request));
        return ResponseEntity.accepted().body(ApiResponse.ok(response,
                "Registration verification request accepted."));
    }

    @GetMapping("/register/status")
    public ResponseEntity<ApiResponse<RegistrationEmailStatusResponse>> getRegistrationEmailStatus(
            @RequestParam UUID challengeId) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.getRegistrationEmailStatus(challengeId)));
    }

    @PostMapping("/register/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyRegistration(
            @Valid @RequestBody VerifyRegistrationRequest request) {
        AuthResponse response = registrationService.verifyRegistration(
                request.getEmail(), request.getChallengeId(), request.getOtp());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response, "Email verified; account created."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<ForgotPasswordChallengeResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordChallengeResponse response = passwordResetService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(response, "Password reset code sent successfully."));
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyResetOtp(
            @Valid @RequestBody VerifyResetOtpRequest request) {
        boolean valid = passwordResetService.verifyOtp(request.getChallengeToken(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("verified", valid), "OTP verified successfully."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Password has been reset successfully. You can now log in."));
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
