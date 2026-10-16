package com.rrjaggery.auth.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registration_email_otps", schema = "auth_schema")
public class RegistrationEmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "challenge_token", nullable = false, unique = true)
    private UUID challengeToken;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(name = "business_name", length = 150)
    private String businessName;

    @Column(length = 20)
    private String gstin;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    @Column(name = "send_window_started_at", nullable = false)
    private Instant sendWindowStartedAt;

    @Column(name = "send_count", nullable = false)
    private int sendCount;

    @Column(nullable = false)
    private int attempts;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_delivery_status", nullable = false, length = 20)
    private RegistrationEmailStatus emailDeliveryStatus = RegistrationEmailStatus.PENDING;

    public RegistrationEmailOtp() {}

    public UUID getId() { return id; }
    public UUID getChallengeToken() { return challengeToken; }
    public void setChallengeToken(UUID challengeToken) { this.challengeToken = challengeToken; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(Instant lastSentAt) { this.lastSentAt = lastSentAt; }
    public Instant getSendWindowStartedAt() { return sendWindowStartedAt; }
    public void setSendWindowStartedAt(Instant sendWindowStartedAt) { this.sendWindowStartedAt = sendWindowStartedAt; }
    public int getSendCount() { return sendCount; }
    public void setSendCount(int sendCount) { this.sendCount = sendCount; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public RegistrationEmailStatus getEmailDeliveryStatus() { return emailDeliveryStatus; }
    public void setEmailDeliveryStatus(RegistrationEmailStatus emailDeliveryStatus) {
        this.emailDeliveryStatus = emailDeliveryStatus;
    }
}
