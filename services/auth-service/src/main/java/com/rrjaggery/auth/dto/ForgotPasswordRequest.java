package com.rrjaggery.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "A valid email is required")
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
