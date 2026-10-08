package com.rrjaggery.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateEmployeeRequest {

    private String employeeCode;

    @NotBlank(message = "Employee name is required")
    private String fullName;

    @NotBlank(message = "Designation is required")
    private String designation;

    private String phone;
    private String wageType = "DAILY"; // MONTHLY, DAILY, BATCH_BASED, PER_KG, CONTRACT

    @NotNull(message = "Rate amount is required")
    @DecimalMin(value = "0.00", message = "Rate amount cannot be negative")
    private BigDecimal rateAmount = BigDecimal.ZERO;

    private LocalDate joiningDate = LocalDate.now();
    private String notes;

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

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

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
