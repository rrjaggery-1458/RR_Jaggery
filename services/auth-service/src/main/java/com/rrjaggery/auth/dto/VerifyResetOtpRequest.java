package com.rrjaggery.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class VerifyResetOtpRequest {

    @NotNull(message = "Challenge token is required")
    private UUID challengeToken;

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
    private String otp;

    public UUID getChallengeToken() { return challengeToken; }
    public void setChallengeToken(UUID challengeToken) { this.challengeToken = challengeToken; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}
