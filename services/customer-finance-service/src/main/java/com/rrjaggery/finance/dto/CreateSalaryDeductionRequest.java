package com.rrjaggery.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class CreateSalaryDeductionRequest {

    @NotNull(message = "Employee ID is required")
    private UUID employeeId;

    @NotNull(message = "Period year is required")
    private int periodYear;

    @NotNull(message = "Period month is required")
    private int periodMonth;

    @NotNull(message = "Deduction date is required")
    private LocalDate deductionDate = LocalDate.now();

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Deduction amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String notes;
    private String createdBy;

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public int getPeriodYear() { return periodYear; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }

    public int getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(int periodMonth) { this.periodMonth = periodMonth; }

    public LocalDate getDeductionDate() { return deductionDate; }
    public void setDeductionDate(LocalDate deductionDate) { this.deductionDate = deductionDate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
