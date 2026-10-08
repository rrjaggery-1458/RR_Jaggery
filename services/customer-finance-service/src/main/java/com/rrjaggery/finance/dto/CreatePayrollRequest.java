package com.rrjaggery.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class CreatePayrollRequest {

    @NotNull(message = "Employee ID is required")
    private UUID employeeId;

    @NotNull(message = "Period month is required")
    private int periodMonth;

    @NotNull(message = "Period year is required")
    private int periodYear;

    private BigDecimal daysWorked = BigDecimal.ZERO;
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @NotNull(message = "Base amount is required")
    @DecimalMin(value = "0.00", message = "Base amount cannot be negative")
    private BigDecimal baseAmount = BigDecimal.ZERO;

    private BigDecimal bonusAllowance = BigDecimal.ZERO;
    private BigDecimal deductions = BigDecimal.ZERO;

    @NotNull(message = "Net amount paid is required")
    @DecimalMin(value = "0.01", message = "Net amount paid must be greater than 0")
    private BigDecimal netAmountPaid;

    private LocalDate paymentDate = LocalDate.now();
    private String paymentMethod = "CASH";
    private String referenceNotes;

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public int getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(int periodMonth) { this.periodMonth = periodMonth; }

    public int getPeriodYear() { return periodYear; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }

    public BigDecimal getDaysWorked() { return daysWorked; }
    public void setDaysWorked(BigDecimal daysWorked) { this.daysWorked = daysWorked; }

    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }

    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }

    public BigDecimal getBonusAllowance() { return bonusAllowance; }
    public void setBonusAllowance(BigDecimal bonusAllowance) { this.bonusAllowance = bonusAllowance; }

    public BigDecimal getDeductions() { return deductions; }
    public void setDeductions(BigDecimal deductions) { this.deductions = deductions; }

    public BigDecimal getNetAmountPaid() { return netAmountPaid; }
    public void setNetAmountPaid(BigDecimal netAmountPaid) { this.netAmountPaid = netAmountPaid; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getReferenceNotes() { return referenceNotes; }
    public void setReferenceNotes(String referenceNotes) { this.referenceNotes = referenceNotes; }
}
