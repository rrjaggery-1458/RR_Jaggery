package com.rrjaggery.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_otps", schema = "auth_schema",
        indexes = @Index(name = "idx_pwr_email", columnList = "email"))
public class PasswordResetOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Public token sent to the client so we can look up the challenge without leaking the email. */
    @Column(name = "challenge_token", nullable = false, unique = true)
    private UUID challengeToken;

    @Column(nullable = false, length = 150)
    private String email;

    /** BCrypt hash of the 6-digit OTP. */
    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /** Throttle: track failed verification attempts. */
    @Column(nullable = false)
    private int attempts;

    /** Whether the OTP has been verified (one-time use after which the user can set a new password). */
    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    public PasswordResetOtp() {}

    public UUID getId() { return id; }
    public UUID getChallengeToken() { return challengeToken; }
    public void setChallengeToken(UUID challengeToken) { this.challengeToken = challengeToken; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}
