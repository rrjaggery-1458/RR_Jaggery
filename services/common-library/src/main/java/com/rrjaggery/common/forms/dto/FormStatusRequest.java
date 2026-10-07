package com.rrjaggery.common.forms.dto;

import jakarta.validation.constraints.NotNull;

public record FormStatusRequest(@NotNull Boolean active) {
}
