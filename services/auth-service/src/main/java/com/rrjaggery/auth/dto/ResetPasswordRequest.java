package com.rrjaggery.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class ResetPasswordRequest {

    @NotNull(message = "Challenge token is required")
    private UUID challengeToken;

    private String otp;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;

    public UUID getChallengeToken() { return challengeToken; }
    public void setChallengeToken(UUID challengeToken) { this.challengeToken = challengeToken; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
