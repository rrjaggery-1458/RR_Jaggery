package com.rrjaggery.common.forms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FormDefinitionRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,79}$")
        String formKey,
        @NotBlank @Size(max = 160) String displayName,
        @Size(max = 1000) String description) {
}
