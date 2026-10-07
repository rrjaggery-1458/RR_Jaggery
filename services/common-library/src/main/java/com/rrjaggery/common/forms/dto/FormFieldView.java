package com.rrjaggery.common.forms.dto;

import com.rrjaggery.common.forms.FieldType;

import java.util.List;
import java.util.UUID;

public record FormFieldView(
        UUID id,
        String fieldKey,
        String label,
        FieldType fieldType,
        boolean required,
        boolean active,
        boolean systemManaged,
        int displayOrder,
        String placeholder,
        String helpText,
        String validationJson,
        List<FormOptionView> options) {
}
