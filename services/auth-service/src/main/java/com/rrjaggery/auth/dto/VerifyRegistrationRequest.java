package com.rrjaggery.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public class VerifyRegistrationRequest {

    @NotBlank
    @Email
    private String email;

    @NotNull
    private UUID challengeId;

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "OTP must contain 6 digits")
    private String otp;

    public VerifyRegistrationRequest() {}
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UUID getChallengeId() { return challengeId; }
    public void setChallengeId(UUID challengeId) { this.challengeId = challengeId; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}
