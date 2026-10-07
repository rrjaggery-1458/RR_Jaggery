package com.rrjaggery.common.forms.dto;

import com.rrjaggery.common.forms.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FormFieldRequest(
        @NotBlank @Pattern(regexp = "^[a-z][a-zA-Z0-9_]{0,99}$") String fieldKey,
        @NotBlank @Size(max = 160) String label,
        @NotNull FieldType fieldType,
        boolean required,
        int displayOrder,
        @Size(max = 160) String placeholder,
        @Size(max = 1000) String helpText,
        @Size(max = 1000) String validationJson) {
}
