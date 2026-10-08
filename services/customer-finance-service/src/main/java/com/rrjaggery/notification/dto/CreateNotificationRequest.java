package com.rrjaggery.notification.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class CreateNotificationRequest {
    private UUID userId;
    private String role; // "ADMIN", "MANAGER", "CUSTOMER", "ALL"

    @NotBlank(message = "Notification type is required")
    private String type; // "LOW_STOCK", "PENDING_BILL", "RECEIVABLE_DUE", "PENDING_ORDER", "PRODUCTION_ALERT", "PAYROLL_REMINDER", "SYSTEM"

    private String severity = "INFO"; // "INFO", "WARNING", "CRITICAL", "SUCCESS"

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private String referenceId;
    private String referenceType;

    public CreateNotificationRequest() {}

    public CreateNotificationRequest(UUID userId, String role, String type, String severity, String title, String message, String referenceId, String referenceType) {
        this.userId = userId;
        this.role = role;
        this.type = type;
        this.severity = severity != null ? severity : "INFO";
        this.title = title;
        this.message = message;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }
}
