package com.rrjaggery.common.forms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FormOptionRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_.:-]{0,119}$") String value,
        @NotBlank @Size(max = 160) String label,
        int displayOrder) {
}
