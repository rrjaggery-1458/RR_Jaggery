package com.rrjaggery.common.forms.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FormDefinitionView(
        UUID id,
        String formKey,
        String displayName,
        String description,
        boolean active,
        int version,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy,
        List<FormFieldView> fields) {
}
