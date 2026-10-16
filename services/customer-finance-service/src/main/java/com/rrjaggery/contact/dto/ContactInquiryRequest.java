package com.rrjaggery.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ContactInquiryRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 254, message = "Email must be at most 254 characters")
    private String email;

    @Pattern(
            regexp = "^(?:(?=.{7,20}$)\\+?[0-9](?:[0-9\\s().-]*[0-9])?)?$",
            message = "Phone must contain 7 to 20 characters and use a valid phone number format"
    )
    private String phone;

    @NotBlank(message = "Message is required")
    @Size(min = 10, max = 5000, message = "Message must be between 10 and 5000 characters")
    private String message;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
