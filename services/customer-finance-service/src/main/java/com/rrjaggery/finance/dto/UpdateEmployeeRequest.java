package com.rrjaggery.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateEmployeeRequest {

    @NotBlank(message = "Employee name is required")
    private String fullName;

    @NotBlank(message = "Designation is required")
    private String designation;

    private String phone;
    private String wageType; // DAILY, WEEKLY, MONTHLY, BATCH_BASED; legacy PER_KG and CONTRACT are retained.

    @NotNull(message = "Rate amount is required")
    @DecimalMin(value = "0.00", message = "Rate amount cannot be negative")
    private BigDecimal rateAmount;

    private LocalDate joiningDate;
    private boolean active = true;
    private String notes;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getWageType() { return wageType; }
    public void setWageType(String wageType) { this.wageType = wageType; }

    public BigDecimal getRateAmount() { return rateAmount; }
    public void setRateAmount(BigDecimal rateAmount) { this.rateAmount = rateAmount; }

    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
