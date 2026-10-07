package com.rrjaggery.common.forms.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Map;
import java.util.UUID;

public record FormSubmissionRequest(
        @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,79}$") String formKey,
        @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,79}$") String recordType,
        UUID recordId,
        @NotNull Map<String, JsonNode> answers) {
}
