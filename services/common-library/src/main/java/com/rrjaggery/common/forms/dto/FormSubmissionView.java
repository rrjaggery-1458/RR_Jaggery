package com.rrjaggery.common.forms.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record FormSubmissionView(
        UUID id,
        String formKey,
        UUID formRevisionId,
        int formVersion,
        String recordType,
        UUID recordId,
        Map<String, JsonNode> answers,
        String submittedBy,
        Instant submittedAt,
        JsonNode revisionSnapshot) {
}
