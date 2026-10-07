package com.rrjaggery.common.forms.dto;

import java.util.UUID;

public record FormOptionView(
        UUID id,
        String value,
        String label,
        int displayOrder,
        boolean active) {
}
