package com.rrjaggery.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInvestmentRequest(
        @NotBlank @Size(max = 200) String investorName,
        @NotBlank @Size(max = 120) String investmentType,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotNull LocalDate investmentDate,
        @Size(max = 1000) String details) {
}
